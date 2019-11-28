package io.vertx.starter.database.services;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.starter.database.enums.SqlQuery;

import java.util.Map;

@ProxyGen
@VertxGen
public interface WikiDatabaseService {
  @GenIgnore
  static WikiDatabaseService create(JDBCClient dbClient, Map<SqlQuery, String> sqlQueries, Handler<AsyncResult<WikiDatabaseService>> readyHandler) {
    return new WikiDatabaseServiceImpl(dbClient, sqlQueries, readyHandler);
  }

  @GenIgnore
  static WikiDatabaseService createProxy(Vertx vertx, String address) {
    return new WikiDatabaseServiceVertxEBProxy(vertx, address);
  }

  void fetchAllPages(Handler<AsyncResult<JsonArray>> resultHandler);

  void fetchPage(JsonObject request, Handler<AsyncResult<JsonArray>> resultHandler);

  void savePage(JsonObject request, Handler<AsyncResult<JsonArray>> resultHandler);

  void deletePage(JsonObject request, Handler<AsyncResult<JsonArray>> resultHandler);
}
