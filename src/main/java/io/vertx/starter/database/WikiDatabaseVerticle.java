package io.vertx.starter.database;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.starter.database.enums.SqlQuery;
import io.vertx.starter.database.services.WikiDatabaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Map;

public class WikiDatabaseVerticle extends AbstractVerticle {
  private static final Logger log = LogManager.getLogger(WikiDatabaseVerticle.class);
  private static final String CONFIG_WIKIDB_JDBC_URL = "wikidb.jdbc.url";
  private static final String CONFIG_WIKIDB_JDBC_DRIVER_CLASS = "wikidb.jdbc.driver_class";
  private static final String CONFIG_WIKIDB_JDBC_MAX_POOL_SIZE = "wikidb.jdbc.max_pool_size";
  private static final String DB_SERVICE_ADDRESS = "database-service-address";
  private Map<SqlQuery, String> sqlQueries;
  private JDBCClient dbClient;
  private boolean isInitialized = false;

  @Override
  public void init(Vertx vertx, Context context) {
    try {
      super.init(vertx, context);
      sqlQueries = SqlLoader.loadSqlQueries(config());
      isInitialized = true;
    } catch (IOException e) {
      isInitialized = false;
    }
  }

  @Override
  public void start(Promise<Void> promise) {
    if(!isInitialized){
      promise.fail("Initialization failed");
    }

    dbClient = JDBCClient.createShared(vertx, new JsonObject()
      .put("url", config().getString(CONFIG_WIKIDB_JDBC_URL, "jdbc:hsqldb:file:db/wiki"))
      .put("driver_class", config().getString(CONFIG_WIKIDB_JDBC_DRIVER_CLASS, "org.hsqldb.jdbcDriver"))
      .put("max_pool_size", config().getInteger(CONFIG_WIKIDB_JDBC_MAX_POOL_SIZE, 30)));

    WikiDatabaseService.create(dbClient, sqlQueries, serviceResult -> {
      if (serviceResult.succeeded()) {
        WikiDatabaseService wikiDatabaseService = serviceResult.result();
        new ServiceBinder(vertx)
          .setAddress(DB_SERVICE_ADDRESS)
          .register(WikiDatabaseService.class, wikiDatabaseService);
        promise.complete();
      } else {
        promise.fail(serviceResult.cause());
      }
    });
  }
}

