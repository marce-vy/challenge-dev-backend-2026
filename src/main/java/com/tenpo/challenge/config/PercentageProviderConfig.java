package com.tenpo.challenge.config;

import com.tenpo.challenge.application.port.out.PercentageProviderPort;
import com.tenpo.challenge.external.percentage.ExternalPercentageProvider;
import com.tenpo.challenge.external.percentage.PercentageFetcher;
import com.tenpo.challenge.infrastructure.RetryingPercentageFetcher;
import io.github.resilience4j.retry.Retry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PercentageProviderConfig {

  private final PercentageProviderProperties properties;

  public PercentageProviderConfig(PercentageProviderProperties properties) {
    this.properties = properties;
  }

  @Bean
  public PercentageFetcher percentageFetcher(
      org.springframework.web.client.RestClient springPercentageRestClient,
      Retry percentageRestClientRetry) {
    return new RetryingPercentageFetcher(
        springPercentageRestClient, percentageRestClientRetry, properties.http().path());
  }

  @Bean
  public PercentageProviderPort percentageProvider(PercentageFetcher percentageFetcher) {
    return new ExternalPercentageProvider(percentageFetcher);
  }
}
