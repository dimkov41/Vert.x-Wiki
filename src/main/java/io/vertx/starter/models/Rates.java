package io.vertx.starter.models;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.starter.models.codecs.RateMessageCodec;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

/**
 * @author Radostin Dimkov on 16.12.19
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Rates extends RateMessageCodec {
  @JsonProperty("base")
  private String base;

  @JsonProperty("date")
  private Date date;

  @JsonProperty("rates")
  private Map<String, BigDecimal> rates;

  public String getBase() {
    return base;
  }

  public void setBase(String base) {
    this.base = base;
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public Map<String, BigDecimal> getRates() {
    return rates;
  }

  public void setRates(Map<String, BigDecimal> rates) {
    this.rates = rates;
  }

  @Override
  public String toString() {
    return "Rates{" +
      "base='" + base + '\'' +
      ", date=" + date +
      ", rates=" + rates +
      '}';
  }
}
