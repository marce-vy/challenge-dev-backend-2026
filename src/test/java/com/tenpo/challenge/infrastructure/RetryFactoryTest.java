package com.tenpo.challenge.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.core.functions.Either;
import io.github.resilience4j.retry.Retry;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

class RetryFactoryTest {

  @Test
  void createsRetryWithThreeTotalAttemptsAndBackoff() {
    Retry retry =
        RetryFactory.withExponentialBackoff(
            "test-percentage-provider", 3, Duration.ofMillis(50), 2.0);

    io.github.resilience4j.retry.RetryConfig retryConfig = retry.getRetryConfig();
    Either<Throwable, Object> failure = Either.left(new IllegalStateException("failure"));

    assertThat(retryConfig.getMaxAttempts()).isEqualTo(3);
    assertThat(retryConfig.getIntervalBiFunction().apply(1, failure)).isEqualTo(50L);
    assertThat(retryConfig.getIntervalBiFunction().apply(2, failure)).isGreaterThan(50L);
  }

  @Test
  void retriesTransientExceptionsOnly() {
    Retry retry =
        RetryFactory.withExponentialBackoff(
            "test-percentage-provider", 3, Duration.ofMillis(50), 2.0);

    assertThat(
            retry.getRetryConfig().getExceptionPredicate().test(new java.io.IOException("timeout")))
        .isTrue();
    assertThat(
            retry.getRetryConfig().getExceptionPredicate().test(new RestClientException("timeout")))
        .isTrue();
    assertThat(
            retry
                .getRetryConfig()
                .getExceptionPredicate()
                .test(new IllegalArgumentException("invalid input")))
        .isFalse();
    assertThat(retry.getRetryConfig().getExceptionPredicate().test(new NullPointerException()))
        .isFalse();
  }
}
