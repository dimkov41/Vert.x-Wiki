package io.vertx.starter.http;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.serviceproxy.ServiceProxyBuilder;
import io.vertx.starter.common.Constants;
import io.vertx.starter.database.services.WikiDatabaseService;

public class HttpBackupVerticle extends AbstractVerticle {
  private WikiDatabaseService dbService;

  @Override
  public void start(Promise<Void> promise) {
    ServiceProxyBuilder builder = new ServiceProxyBuilder(vertx).setAddress(Constants.DB_SERVICE_ADDRESS);
    dbService = builder.build(WikiDatabaseService.class);


  }
}
