package com.tenpo.challenge.config;

import com.tenpo.challenge.infrastructure.RetryFactory;
import io.github.resilience4j.retry.Retry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RetryConfig {

  @Bean
  public Retry percentageRestClientRetry(PercentageProviderProperties properties) {
    return retry(properties.retry());
  }

  public Retry retry(RetryProperties retryProperties) {
    return RetryFactory.withExponentialBackoff(
        retryProperties.name(),
        retryProperties.maxAttempts(),
        retryProperties.initialBackoff(),
        retryProperties.backoffMultiplier());
  }
}
