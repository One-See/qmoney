package com.crio.warmup.stock.dto;

import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonProperty;

// TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
//  Implement the Candle interface in such a way that it matches the parameters returned
//  inside Json response from Alphavantage service.

  // Reference - https:www.baeldung.com/jackson-ignore-properties-on-serialization
  // Reference - https:www.baeldung.com/jackson-name-of-property
public class AlphavantageCandle implements Candle {
  @JsonProperty("1. open")
  private Double open;
  private Double close;
  private Double high;
  private Double low;
  private LocalDate date;
  @Override
  public Double getOpen() {
    // TODO Auto-generated method stub
    return this.open;
  }
  @Override
  public Double getClose() {
    // TODO Auto-generated method stub
    return this.close;
  }
  @Override
  public Double getHigh() {
    // TODO Auto-generated method stub
    return this.high;
  }
  @Override
  public Double getLow() {
    // TODO Auto-generated method stub
    return this.low;
  }
  @Override
  public LocalDate getDate() {
    // TODO Auto-generated method stub
    return this.date;
  }
}

