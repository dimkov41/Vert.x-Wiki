package io.vertx.starter.database;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.starter.database.enums.SqlQuery;
import io.vertx.starter.database.services.WikiDatabaseService;

import java.util.Map;

public class WikiDatabaseVerticle extends AbstractVerticle {
  private static final String CONFIG_WIKIDB_JDBC_URL = "wikidb.jdbc.url";
  private static final String CONFIG_WIKIDB_JDBC_DRIVER_CLASS = "wikidb.jdbc.driver_class";
  private static final String CONFIG_WIKIDB_JDBC_MAX_POOL_SIZE = "wikidb.jdbc.max_pool_size";
  private static final String DB_SERVICE_ADDRESS = "database-service-address";
  private static Map<SqlQuery, String> sqlQueries = null;

  private JDBCClient dbClient;

  @Override
  public void start(Promise<Void> promise) {
    vertx.executeBlocking(this::loadSqlQueries, result -> {

      if(result.succeeded()) {
        sqlQueries = result.result();
        dbClient = JDBCClient.createShared(vertx, new JsonObject()
          .put("url", config().getString(CONFIG_WIKIDB_JDBC_URL, "jdbc:hsqldb:file:db/wiki"))
          .put("driver_class", config().getString(CONFIG_WIKIDB_JDBC_DRIVER_CLASS, "org.hsqldb.jdbcDriver"))
          .put("max_pool_size", config().getInteger(CONFIG_WIKIDB_JDBC_MAX_POOL_SIZE, 30)));

        WikiDatabaseService.create(dbClient, sqlQueries, serviceResult -> {
          if(serviceResult.succeeded()){
            WikiDatabaseService wikiDatabaseService = serviceResult.result();
            new ServiceBinder(vertx)
              .setAddress(DB_SERVICE_ADDRESS)
              .register(WikiDatabaseService.class, wikiDatabaseService);
            promise.complete();
          } else {
            promise.fail(serviceResult.cause());
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
}
