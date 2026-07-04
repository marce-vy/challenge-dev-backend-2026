package com.tenpo.challenge.config;

import com.tenpo.challenge.application.port.out.PercentageProviderPort;
import com.tenpo.challenge.application.port.out.RetryStrategy;
import com.tenpo.challenge.external.percentage.ExternalPercentageProvider;
import com.tenpo.challenge.external.percentage.PercentageFetcher;
import com.tenpo.challenge.infrastructure.RetryingPercentageFetcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class PercentageProviderConfig {

  private final PercentageProviderProperties properties;

  public PercentageProviderConfig(PercentageProviderProperties properties) {
    this.properties = properties;
  }

  @Bean
  public PercentageFetcher percentageFetcher(
      WebClient springPercentageWebClient, RetryStrategy percentageRetryStrategy) {
    return new RetryingPercentageFetcher(
        springPercentageWebClient, percentageRetryStrategy, properties.http().path());
  }

  @Bean
  public PercentageProviderPort percentageProvider(PercentageFetcher percentageFetcher) {
    return new ExternalPercentageProvider(percentageFetcher);
  }
}
