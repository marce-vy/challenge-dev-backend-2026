package com.tenpo.challenge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "call-history.async")
public record CallHistoryAsyncProperties(int concurrencyLimit) {

  public CallHistoryAsyncProperties {
    if (concurrencyLimit <= 0) {
      throw new IllegalArgumentException("concurrencyLimit must be greater than zero");
    }
  }
}
