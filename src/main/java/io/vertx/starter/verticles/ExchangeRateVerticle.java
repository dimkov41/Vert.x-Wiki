package io.vertx.starter.verticles;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.starter.models.Rates;
import io.vertx.starter.models.codecs.RateMessageCodec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static io.vertx.starter.common.Constants.EXCHANGE_RATE_ADDRESS;

public class ExchangeRateVerticle extends AbstractVerticle {
  private static final Logger log = LogManager.getLogger(ExchangeRateVerticle.class);
  private WebClient webClient;
  private EventBus eventBus;
  private RateMessageCodec messageCodec;

  @Override
  public void init(Vertx vertx, Context context) {
    super.init(vertx, context);
    this.webClient = WebClient.create(vertx);
    this.eventBus = vertx.eventBus();
    this.messageCodec = new RateMessageCodec();
    eventBus.registerCodec(messageCodec);
  }

  @Override
  public void start(Promise<Void> promise) {
    webClient.getAbs("https://api.exchangeratesapi.io/latest")
      .addQueryParam("base", "BGN")
      .send(ar -> {
        if (ar.succeeded()) {
          HttpResponse<Buffer> httpResponse = ar.result();
          Rates rates = httpResponse.bodyAsJson(Rates.class);
          DeliveryOptions options = new DeliveryOptions().setCodecName(messageCodec.name());
          eventBus.send(EXCHANGE_RATE_ADDRESS, rates, options);
          promise.complete();
        } else {
          log.error("Cannot call external api from {}", getClass(), ar.cause());
          promise.fail(ar.cause());
        }
      });
  }
}
