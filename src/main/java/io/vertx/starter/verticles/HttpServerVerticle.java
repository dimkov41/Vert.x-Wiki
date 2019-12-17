package io.vertx.starter.verticles;

import com.github.rjeschke.txtmark.Processor;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.templ.freemarker.FreeMarkerTemplateEngine;
import io.vertx.serviceproxy.ServiceProxyBuilder;
import io.vertx.starter.common.Constants;
import io.vertx.starter.database.services.WikiDatabaseService;
import io.vertx.starter.models.Rates;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;

import static io.vertx.starter.common.Constants.EXCHANGE_RATE_ADDRESS;

public class HttpServerVerticle extends AbstractVerticle {
  private static final Logger log = LogManager.getLogger(HttpServerVerticle.class);
  private static final String EMPTY_PAGE_MARKDOWN = "# A new page\n" + "\n" + "Feel-free to write in Markdown!\n";
  private static final String CONFIG_HTTP_SERVER_PORT = "http.server.port";

  private FreeMarkerTemplateEngine templateEngine;
  private WikiDatabaseService dbService;
  private Rates rates;

  @Override
  public void init(Vertx vertx, Context context) {
    super.init(vertx, context);
    ServiceProxyBuilder builder = new ServiceProxyBuilder(vertx).setAddress(Constants.DB_SERVICE_ADDRESS);
    dbService = builder.build(WikiDatabaseService.class);
    vertx.eventBus().consumer(EXCHANGE_RATE_ADDRESS, m -> rates = (Rates) m.body());
  }

  @Override
  public void start(Promise<Void> promise) {
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
        promise.complete();
        log.info("Server listening on port {}", serverPort);
      } else {
        promise.fail(result.cause());
      }
    });
  }

  private void homeHandler(RoutingContext context) {
    dbService.fetchAllPages(result -> {
      if (result.succeeded()) {
        JsonArray pages = result.result();
        context.put("title", "Wiki home");
        context.put("pages", pages.getList());
        context.put("rates", rates);
        templateEngine.render(context.data(), "templates/index.ftl", ar -> {
          if (ar.succeeded()) {
            context.response().putHeader("Content-type", "text/html");
            context.response().end(ar.result());
          } else {
            context.fail(result.cause());
            log.error("Failed to render index.ftl", result.cause());
          }
        });
      } else {
        context.fail(result.cause());
      }
    });
  }

  private void pageRenderingHandler(RoutingContext context) {
    String requestedPage = context.request().getParam("page");
    JsonObject request = new JsonObject().put("page", requestedPage);

    dbService.fetchPage(request, result -> {
      if (result.succeeded()) {
        JsonObject body = result.result().getJsonObject(0);
        String rawContent = body.getString("rawContent", EMPTY_PAGE_MARKDOWN);
        context.put("title", requestedPage);
        context.put("id", body.getInteger("id", -1));
        context.put("newPage", body.getBoolean("newPage").toString());
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
        context.fail(result.cause());
      }
    });
  }

  private void pageUpdateHandler(RoutingContext context) {
    String id = context.request().getParam("id");
    String title = context.request().getParam("title");
    String markdown = context.request().getParam("markdown");
    boolean newPage = "true".equals(context.request().getParam("newPage"));

    JsonObject request = new JsonObject()
      .put("id", id)
      .put("title", title)
      .put("markdown", markdown)
      .put("newPage", newPage);

    dbService.savePage(request, result -> {
      if (result.succeeded()) {
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

    dbService.deletePage(request, result -> {
      if (result.succeeded()) {
        context.response().setStatusCode(303).putHeader("Location", "/").end();
      } else {
        context.fail(result.cause());
      }
    });
  }
}
