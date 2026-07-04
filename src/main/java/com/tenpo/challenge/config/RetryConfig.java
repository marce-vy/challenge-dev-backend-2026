package com.tenpo.challenge.config;

import com.tenpo.challenge.application.port.out.RetryStrategy;
import com.tenpo.challenge.infrastructure.ExponentialBackoffRetryStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RetryConfig {

  @Bean
  public RetryStrategy percentageRetryStrategy(PercentageProviderProperties properties) {
    return retryStrategy(properties.retry());
  }

  public RetryStrategy retryStrategy(RetryProperties retryProperties) {
    return new ExponentialBackoffRetryStrategy(
        retryProperties.name(),
        retryProperties.maxAttempts(),
        retryProperties.initialBackoff(),
        retryProperties.backoffMultiplier());
  }
}
