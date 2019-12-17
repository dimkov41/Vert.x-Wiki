package io.vertx.starter.models.codecs;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.JsonObject;
import io.vertx.starter.models.Rates;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

/**
 * @author Radostin Dimkov on 13.12.19
 */
public class RateMessageCodec implements MessageCodec<Rates, Rates> {
  private static final String BASE = "base";
  private static final String DATE = "date";
  private static final String RATES = "rates";

  @Override
  public void encodeToWire(Buffer buffer, Rates rates) {
      JsonObject jsonObj = new JsonObject();
      jsonObj.put(BASE, rates.getBase());
      jsonObj.put(DATE, rates.getDate());
      jsonObj.put(RATES, rates.getRates());

      String encodedJsonObj = jsonObj.encode();
      int strLength = encodedJsonObj.length();

      buffer.appendInt(strLength);
      buffer.appendString(encodedJsonObj);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Rates decodeFromWire(int position, Buffer buffer) {
    int strLength = buffer.getInt(0);
    String encodedJsonObj = buffer.getString(1, strLength + 1);
    JsonObject jsonObj = new JsonObject(encodedJsonObj);
    Rates rates = new Rates();
    rates.setBase(jsonObj.getString(BASE));
    rates.setDate((Date) jsonObj.getValue(DATE));
    rates.setRates((Map<String, BigDecimal>) jsonObj.getValue(RATES));
    return rates;
  }

  @Override
  public Rates transform(Rates rates) {
    // If a message is sent *locally* across the event bus.
    // This example sends message just as is
    return rates;
  }

  @Override
  public String name() {
    // Each codec must have a unique name.
    // This is used to identify a codec when sending a message and for unregistering codecs.
    return this.getClass().getSimpleName();
  }

  @Override
  public byte systemCodecID() {
    // Always -1
    return -1;
  }
}
