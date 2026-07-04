package com.tenpo.challenge.infrastructure;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import java.time.Duration;
import org.springframework.web.client.RestClientException;

public final class RetryFactory {

  private RetryFactory() {}

  public static Retry withExponentialBackoff(
      String name, int maxAttempts, Duration initialInterval, double multiplier) {
    io.github.resilience4j.retry.RetryConfig retryConfig =
        io.github.resilience4j.retry.RetryConfig.custom()
            .maxAttempts(maxAttempts)
            .intervalFunction(IntervalFunction.ofExponentialBackoff(initialInterval, multiplier))
            .retryExceptions(java.io.IOException.class, RestClientException.class)
            .build();
    return Retry.of(name, retryConfig);
  }
}
