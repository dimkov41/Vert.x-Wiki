package io.vertx.starter;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.templ.freemarker.FreeMarkerTemplateEngine;

/**
 * Each phase can fail (e.g., the HTTP server TCP port is already being used),
 * and they should not run in parallel as the web application code first needs the database access to work.
 */
public class MainVerticle extends AbstractVerticle {
  @Override
  public void start(Promise<Void> promise) {
    Promise<String> dbVerticleDeployment = Promise.promise();
    vertx.deployVerticle("io.vertx.starter.database.WikiDatabaseVerticle", dbVerticleDeployment);

    dbVerticleDeployment.future().compose(id -> {
      Promise<String> httpVerticleDeployment = Promise.promise();
      DeploymentOptions deploymentOptions = new DeploymentOptions();
      deploymentOptions.setInstances(2);
      vertx.deployVerticle("io.vertx.starter.http.HttpServerVerticle", deploymentOptions, httpVerticleDeployment);
      return httpVerticleDeployment.future();
    }).setHandler(result -> {
      if (result.succeeded()) {
        promise.complete();
      } else {
        promise.fail(result.cause());
      }
    });
  }
}
