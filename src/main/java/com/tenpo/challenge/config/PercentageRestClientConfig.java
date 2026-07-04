package com.tenpo.challenge.config;

import com.tenpo.challenge.infrastructure.FailingPercentageExchangeFunction;
import com.tenpo.challenge.infrastructure.MockPercentageExchangeFunction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class PercentageRestClientConfig {

  private final PercentageProviderProperties properties;

  public PercentageRestClientConfig(PercentageProviderProperties properties) {
    this.properties = properties;
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "percentage-provider.transport",
      name = "mode",
      havingValue = "mock",
      matchIfMissing = true)
  public ExchangeFunction mockPercentageExchangeFunction() {
    return new MockPercentageExchangeFunction();
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "percentage-provider.transport", name = "mode", havingValue = "failure")
  public FailingPercentageExchangeFunction failingPercentageExchangeFunction() {
    return new FailingPercentageExchangeFunction();
  }

  @Bean
  public WebClient springPercentageWebClient(ExchangeFunction percentageExchangeFunction) {
    return WebClient.builder()
        .baseUrl(properties.http().baseUrl())
        .exchangeFunction(percentageExchangeFunction)
        .build();
  }
}
