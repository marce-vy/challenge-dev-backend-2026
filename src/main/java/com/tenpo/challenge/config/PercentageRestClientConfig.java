package com.tenpo.challenge.config;

import com.tenpo.challenge.infrastructure.FailingPercentageHttpTransport;
import com.tenpo.challenge.infrastructure.MockPercentageHttpTransport;
import com.tenpo.challenge.infrastructure.RestClientFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;

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
  public MockPercentageHttpTransport mockPercentageHttpTransport() {
    return new MockPercentageHttpTransport();
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "percentage-provider.transport",
      name = "mode",
      havingValue = "failure")
  public FailingPercentageHttpTransport failingPercentageHttpTransport() {
    return new FailingPercentageHttpTransport();
  }

  @Bean
  public org.springframework.web.client.RestClient springPercentageRestClient(
      ClientHttpRequestFactory percentageHttpTransport) {
    return RestClientFactory.springRestClient(properties.http().baseUrl(), percentageHttpTransport);
  }
}
