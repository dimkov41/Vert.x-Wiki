package io.vertx.starter.http;

import com.github.rjeschke.txtmark.Processor;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.templ.freemarker.FreeMarkerTemplateEngine;
import io.vertx.starter.database.services.WikiDatabaseService;

import java.util.Date;

public class HttpServerVerticle extends AbstractVerticle {
  private static final String EMPTY_PAGE_MARKDOWN = "# A new page\n" + "\n" + "Feel-free to write in Markdown!\n";
  private static final String CONFIG_HTTP_SERVER_PORT = "http.server.port";
  private static final String DB_SERVICE_ADDRESS = "database-service-address";

  private String wikiDbQueue = "wikidb.queue";
  private FreeMarkerTemplateEngine templateEngine;
  private WikiDatabaseService dbService;

  @Override
  public void start(Promise<Void> promise) throws Exception {
    dbService = WikiDatabaseService.createProxy(vertx, DB_SERVICE_ADDRESS);

    HttpServer httpServer = vertx.createHttpServer();
    Router router = Router.router(vertx);
    templateEngine = FreeMarkerTemplateEngine.create(vertx);

    router.get("/").handler(this::homeHandler);
    router.get("/wiki/:page").handler(this::pageRenderingHandler);
    router.post().handler(BodyHandler.create());
    router.post("/create").handler(this::createPageHandler);
    router.post("/save").handler(this::pageUpdateHandler);
    router.post("/delete").handler(this::pageDeletionHandler);

    int serverPort = config().getInteger(CONFIG_HTTP_SERVER_PORT, 8080);
    httpServer.requestHandler(router).listen(serverPort, result -> {
      if (result.succeeded()) {
        System.out.println(String.format("Server listening on port {%d}", result.result().actualPort()));
        promise.complete();
      } else {
        promise.fail(result.cause());
      }
    });
  }

  private void homeHandler(RoutingContext routingContext) {
    dbService.fetchAllPages(result -> {
      if (result.succeeded()) {
        JsonArray pages = result.result();
        routingContext.put("title", "Wiki home");
        routingContext.put("pages", pages.getList());
        templateEngine.render(routingContext.data(), "templates/index.ftl", ar -> {
          if (ar.succeeded()) {
            routingContext.response().putHeader("Content-type", "text/html");
            routingContext.response().end(ar.result());
          } else {
            routingContext.fail(result.cause());
            System.out.println("Failed to render index.ftl");
          }
        });
      } else {
        routingContext.fail(result.cause());
      }
    });
  }

  private void pageRenderingHandler(RoutingContext context) {
    String requestedPage = context.request().getParam("page");
    JsonObject request = new JsonObject().put("page", requestedPage);

    DeliveryOptions options = new DeliveryOptions().addHeader("action", "get-page");
    vertx.eventBus().request(wikiDbQueue, request, options, reply -> {

      if (reply.succeeded()) {
        JsonObject body = (JsonObject) reply.result().body();

        boolean found = body.getBoolean("found");
        String rawContent = body.getString("rawContent", EMPTY_PAGE_MARKDOWN);
        context.put("title", requestedPage);
        context.put("id", body.getInteger("id", -1));
        context.put("newPage", found ? "no" : "yes");
        context.put("rawContent", rawContent);
        context.put("content", Processor.process(rawContent));
        context.put("timestamp", new Date().toString());

        templateEngine.render(context.data(), "templates/page.ftl", ar -> {
          if (ar.succeeded()) {
            context.response().putHeader("Content-Type", "text/html");
            context.response().end(ar.result());
          } else {
            context.fail(ar.cause());
          }
        });

      } else {
        context.fail(reply.cause());
      }
    });
  }

  private void pageUpdateHandler(RoutingContext context) {
    String id = context.request().getParam("id");
    String title = context.request().getParam("title");
    String markdown = context.request().getParam("markdown");
    boolean newPage = "yes".equals(context.request().getParam("newPage"));

    DeliveryOptions deliveryOptions = new DeliveryOptions();
    if (newPage) {
      deliveryOptions.addHeader("action", "create-page");
    } else {
      deliveryOptions.addHeader("action", "save-page");
    }

    JsonObject request = new JsonObject()
      .put("id", id)
      .put("title", title)
      .put("markdown", markdown);

    vertx.eventBus().request(wikiDbQueue, request, deliveryOptions, result -> {
      if(result.succeeded()){
        context.response().setStatusCode(303).putHeader("Location", "/wiki/" + title).end();
      } else {
        context.fail(result.cause());
      }
    });
  }

  private void createPageHandler(RoutingContext context) {
    String page = context.request().getParam("name");
    String location = "/wiki/" + page;
    if (page == null || page.isEmpty()) {
      location = "/";
    }
    context.response().setStatusCode(303);
    context.response().putHeader("Location", location);
    context.response().end();
  }

  private void pageDeletionHandler(RoutingContext context) {
    String id = context.request().getParam("id");
    JsonObject request = new JsonObject().put("id", id);
    DeliveryOptions options = new DeliveryOptions().addHeader("action", "delete-page");
    vertx.eventBus().request(wikiDbQueue, request, options, reply -> {
      if (reply.succeeded()) {
        context.response().setStatusCode(303);
        context.response().putHeader("Location", "/");
        context.response().end();
      } else {
        context.fail(reply.cause());
      }
    });
  }
}
