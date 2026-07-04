package com.tenpo.challenge.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.core.functions.Either;
import io.github.resilience4j.retry.Retry;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class RetryConfigTest {

  @Test
  void createsRetryFromGenericProperties() {
    RetryProperties properties =
        new RetryProperties("configured-client", 4, Duration.ofMillis(25), 3.0);

    Retry retry = new RetryConfig().retry(properties);

    io.github.resilience4j.retry.RetryConfig retryConfig = retry.getRetryConfig();
    Either<Throwable, Object> failure = Either.left(new IllegalStateException("failure"));

    assertThat(retry.getName()).isEqualTo("configured-client");
    assertThat(retryConfig.getMaxAttempts()).isEqualTo(4);
    assertThat(retryConfig.getIntervalBiFunction().apply(1, failure)).isEqualTo(25L);
    assertThat(retryConfig.getIntervalBiFunction().apply(2, failure)).isGreaterThan(25L);
  }

  @Test
  void createsPercentageRestClientRetryFromPercentageProviderProperties() {
    PercentageProviderProperties properties =
        new PercentageProviderProperties(
            new PercentageProviderProperties.Http("https://example.test", "/percentage"),
            new RetryProperties("percentage-client", 3, Duration.ofMillis(100), 2.0));

    Retry retry = new RetryConfig().percentageRestClientRetry(properties);

    assertThat(retry.getName()).isEqualTo("percentage-client");
    assertThat(retry.getRetryConfig().getMaxAttempts()).isEqualTo(3);
  }
}
