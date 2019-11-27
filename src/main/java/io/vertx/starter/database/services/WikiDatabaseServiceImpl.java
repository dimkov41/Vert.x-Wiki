package io.vertx.starter.database.services;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.starter.database.enums.SqlQuery;

import java.util.Map;
import java.util.stream.Collectors;

public class WikiDatabaseServiceImpl implements WikiDatabaseService {
  private final Map<SqlQuery, String> sqlQueries;
  private final JDBCClient dbClient;

  WikiDatabaseServiceImpl(JDBCClient dbClient, Map<SqlQuery, String> sqlQueries, Handler<AsyncResult<WikiDatabaseService>> readyHandler) {
    this.dbClient = dbClient;
    this.sqlQueries = sqlQueries;

    dbClient.getConnection(ar -> {
      if (ar.failed()) {
        System.out.println("Could not open a database connection " + ar.cause());
        readyHandler.handle(Future.failedFuture(ar.cause()));
      } else {
        SQLConnection connection = ar.result();
        connection.execute(sqlQueries.get(SqlQuery.CREATE_PAGES_TABLE), create -> {
          connection.close();
          if (create.failed()) {
            System.err.println("Database preparation error " + create.cause());
            readyHandler.handle(Future.failedFuture(create.cause()));
          } else {
            readyHandler.handle(Future.succeededFuture(this));
          }
        });
      }
    });
  }

  @Override
  public WikiDatabaseService fetchAllPages(Handler<AsyncResult<JsonArray>> resultHandler) {
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
    return this;
  }
}
