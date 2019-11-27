package io.vertx.starter.database;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.starter.database.enums.DBErrorCodes;
import io.vertx.starter.database.enums.SqlQuery;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WikiDatabaseVerticle extends AbstractVerticle {
  private static final String CONFIG_WIKIDB_JDBC_URL = "wikidb.jdbc.url";
  private static final String CONFIG_WIKIDB_JDBC_DRIVER_CLASS = "wikidb.jdbc.driver_class";
  private static final String CONFIG_WIKIDB_JDBC_MAX_POOL_SIZE = "wikidb.jdbc.max_pool_size";
  private static final String CONFIG_WIKIDB_QUEUE = "wikidb.queue";
  private static Map<SqlQuery, String> sqlQueries = null;

  private JDBCClient dbClient;

  @Override
  public void start(Promise<Void> promise) throws Exception {
    vertx.executeBlocking(this::loadSqlQueries, result -> {

      if(result.succeeded()) {
        sqlQueries = result.result();
        dbClient = JDBCClient.createShared(vertx, new JsonObject()
          .put("url", config().getString(CONFIG_WIKIDB_JDBC_URL, "jdbc:hsqldb:file:db/wiki"))
          .put("driver_class", config().getString(CONFIG_WIKIDB_JDBC_DRIVER_CLASS, "org.hsqldb.jdbcDriver"))
          .put("max_pool_size", config().getInteger(CONFIG_WIKIDB_JDBC_MAX_POOL_SIZE, 30)));

        dbClient.getConnection(ar -> {
          if (ar.succeeded()) {
            SQLConnection connection = ar.result();
            connection.execute(sqlQueries.get(SqlQuery.CREATE_PAGES_TABLE), create -> {
              connection.close();
              if (create.succeeded()) {
                vertx.eventBus().consumer(config().getString(CONFIG_WIKIDB_QUEUE, CONFIG_WIKIDB_QUEUE), this::onMessage);
                promise.complete();
              } else {
                System.err.println("Database preparation error " + create.cause());
                promise.fail(create.cause());
              }
            });
          } else {
            System.err.println("Could not open a database connection " + ar.cause());
            promise.fail(ar.cause());
          }
        });
      } else {
        promise.fail(result.cause());
      }
    });
  }

  private void loadSqlQueries(Promise<Map<SqlQuery,String>> promise){
      Map<SqlQuery, String> sqlQueries = SqlLoader.loadSqlQueries(config());
      if(sqlQueries != null){
        promise.complete(sqlQueries);
      } else {
        System.err.println("Could not load sql queries");
        promise.fail("Could not load sql queries");
      }
  }

  private void onMessage(Message<JsonObject> message) {
    if (!message.headers().contains("action")) {
      System.err.println(String.format("No action header specified for message with headers %s and body %s", message.headers(), message.body().encodePrettily()));
      message.fail(DBErrorCodes.NO_ACTION_SPECIFIED.ordinal(), "No action header specified");
      return;
    }

    String action = message.headers().get("action");
    switch (action) {
      case "all-pages":
        fetchAllPages(message);
        break;
      case "get-page":
        fetchPage(message);
        break;
      case "create-page":
        createPage(message);
        break;
      case "save-page":
        savePage(message);
        break;
      case "delete-page":
        deletePage(message);
        break;
      default:
        message.fail(DBErrorCodes.BAD_ACTION.ordinal(), "Bad action: " + action);
    }
  }

  private void fetchAllPages(Message<JsonObject> message) {
    dbClient.query(sqlQueries.get(SqlQuery.ALL_PAGES), res -> {
      if (res.succeeded()) {
        List<String> pages = res.result()
          .getResults()
          .stream()
          .map(json -> json.getString(0))
          .sorted()
          .collect(Collectors.toList());
        message.reply(new JsonObject().put("pages", new JsonArray(pages)));
      } else {
        reportQueryError(message, res.cause());
      }
    });
  }

  private void fetchPage(Message<JsonObject> message) {
    String requestedPage = message.body().getString("page");
    JsonArray params = new JsonArray().add(requestedPage);

    dbClient.queryWithParams(sqlQueries.get(SqlQuery.GET_PAGE), params, fetch -> {
      if (fetch.succeeded()) {
        JsonObject response = new JsonObject();
        ResultSet resultSet = fetch.result();
        if (resultSet.getNumRows() == 0) {
          response.put("found", false);
        } else {
          response.put("found", true);
          JsonArray row = resultSet.getResults().get(0);
          response.put("id", row.getInteger(0));
          response.put("rawContent", row.getString(1));
        }
        message.reply(response);
      } else {
        reportQueryError(message, fetch.cause());
      }
    });
  }

  private void createPage(Message<JsonObject> message) {
    JsonObject request = message.body();
    JsonArray data = new JsonArray()
      .add(request.getString("title"))
      .add(request.getString("markdown"));

    dbClient.updateWithParams(sqlQueries.get(SqlQuery.CREATE_PAGE), data, res -> {
      if (res.succeeded()) {
        message.reply("ok");
      } else {
        reportQueryError(message, res.cause());
      }
    });
  }

  private void savePage(Message<JsonObject> message) {
    JsonObject request = message.body();
    JsonArray data = new JsonArray()
      .add(request.getString("markdown"))
      .add(request.getString("id"));

    dbClient.updateWithParams(sqlQueries.get(SqlQuery.SAVE_PAGE), data, res -> {
      if (res.succeeded()) {
        message.reply("ok");
      } else {
        reportQueryError(message, res.cause());
      }
    });
  }

  private void deletePage(Message<JsonObject> message) {
    JsonArray data = new JsonArray().add(message.body().getString("id"));

    dbClient.updateWithParams(sqlQueries.get(SqlQuery.DELETE_PAGE), data, res -> {
      if (res.succeeded()) {
        message.reply("ok");
      } else {
        reportQueryError(message, res.cause());
      }
    });
  }

  private void reportQueryError(Message<JsonObject> message, Throwable cause) {
    System.err.println("Database query error " + cause);
    message.fail(DBErrorCodes.DB_ERROR.ordinal(), cause.getMessage());
  }
}
