package io.vertx.starter.database.services;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.starter.database.enums.SqlQuery;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.stream.Collectors;

public class WikiDatabaseServiceImpl implements WikiDatabaseService {
  private static final Logger log = LogManager.getLogger(WikiDatabaseService.class);
  private final Map<SqlQuery, String> sqlQueries;
  private final JDBCClient dbClient;

  WikiDatabaseServiceImpl(JDBCClient dbClient, Map<SqlQuery, String> sqlQueries, Handler<AsyncResult<WikiDatabaseService>> readyHandler) {
    this.dbClient = dbClient;
    this.sqlQueries = sqlQueries;

    dbClient.getConnection(ar -> {
      if (ar.failed()) {
        log.error("Could not open a database connection " + ar.cause());
        readyHandler.handle(Future.failedFuture(ar.cause()));
      } else {
        SQLConnection connection = ar.result();
        connection.execute(this.sqlQueries.get(SqlQuery.CREATE_PAGES_TABLE), create -> {
          connection.close();
          if (create.failed()) {
            log.error("Database preparation error " + create.cause());
            readyHandler.handle(Future.failedFuture(create.cause()));
          } else {
            readyHandler.handle(Future.succeededFuture(this));
          }
        });
      }
    });
  }

  @Override
  public void fetchAllPages(Handler<AsyncResult<JsonArray>> resultHandler) {
    dbClient.query(sqlQueries.get(SqlQuery.ALL_PAGES), res -> {
      if (res.succeeded()) {
        JsonArray pages = new JsonArray(res.result()
          .getResults()
          .stream()
          .map(json -> json.getString(0))
          .sorted()
          .collect(Collectors.toList()));
        resultHandler.handle(Future.succeededFuture(pages));
      } else {
        resultHandler.handle(Future.failedFuture(res.cause()));
      }
    });
  }

  @Override
  public void fetchPage(JsonObject request, Handler<AsyncResult<JsonArray>> resultHandler) {
    String requestedPage = request.getString("page");
    JsonArray params = new JsonArray().add(requestedPage);

    dbClient.queryWithParams(sqlQueries.get(SqlQuery.GET_PAGE), params, res -> {
      if (res.succeeded()) {
        JsonObject response = new JsonObject();
        ResultSet resultSet = res.result();
        if (resultSet.getNumRows() == 0) {
          response.put("newPage", true);
        } else {
          response.put("newPage", false);
          JsonArray row = resultSet.getResults().get(0);
          response.put("id", row.getInteger(0));
          response.put("rawContent", row.getString(1));
        }
        resultHandler.handle(Future.succeededFuture(new JsonArray().add(response)));
      } else {
        resultHandler.handle(Future.failedFuture(res.cause()));
      }
    });
  }

  @Override
  public void savePage(JsonObject request, Handler<AsyncResult<JsonArray>> resultHandler) {
    boolean newPage = request.getBoolean("newPage");
    JsonArray data = new JsonArray();
    if (newPage) {
      data.add(request.getString("title"));
      data.add(request.getString("markdown"));
    } else {
      data.add(request.getString("markdown"));
      data.add(request.getString("id"));
    }

    dbClient.updateWithParams(newPage ? sqlQueries.get(SqlQuery.CREATE_PAGE) : sqlQueries.get(SqlQuery.SAVE_PAGE), data, res -> {
      if (res.succeeded()) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        resultHandler.handle(Future.failedFuture(res.cause()));
      }
    });
  }

  @Override
  public void deletePage(JsonObject request, Handler<AsyncResult<JsonArray>> resultHandler) {
    JsonArray data = new JsonArray()
      .add(request.getString("id"));

    dbClient.updateWithParams(sqlQueries.get(SqlQuery.DELETE_PAGE), data, res -> {
      if (res.succeeded()) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        resultHandler.handle(Future.failedFuture(res.cause()));
      }
    });
  }
}
