package io.vertx.starter;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Each phase can fail (e.g., the HTTP server TCP port is already being used),
 * and they should not run in parallel as the web application code first needs the database access to work.
 */
public class MainVerticle extends AbstractVerticle {
  private static final Logger log = LogManager.getLogger(MainVerticle.class);

  @Override
  public void start(Promise<Void> promise) {
    Promise<String> dbVerticleDeployment = Promise.promise();
    vertx.deployVerticle("io.vertx.starter.database.WikiDatabaseVerticle", dbVerticleDeployment);

    dbVerticleDeployment.future().compose(id -> {
      Promise<String> httpVerticleDeployment = Promise.promise();
      DeploymentOptions deploymentOptions = new DeploymentOptions()
        .setInstances(2);
      vertx.deployVerticle("io.vertx.starter.http.HttpServerVerticle", deploymentOptions, httpVerticleDeployment);
      return httpVerticleDeployment.future();
    }).compose(id -> {
      Promise<String> backupVerticleDeployment = Promise.promise();
//      vertx.deployVerticle("io.vertx.starter.http.HttpBackupVerticle", backupVerticleDeployment);
      backupVerticleDeployment.complete();
      return backupVerticleDeployment.future();
    }).setHandler(result -> {
      if (result.succeeded()) {
        log.info("~~~~~~~~~~All verticles deployed~~~~~~~~~");
        WebClientOptions options = new WebClientOptions()
          .setKeepAlive(true)
          .setPipelining(false);
        WebClient client = WebClient.create(vertx, options);
        promise.complete();
      } else {
        log.error("~~~~~~~~~~Deployment failed~~~~~~~~~", result.cause());
        promise.fail(result.cause());
      }
    });
  }
}
