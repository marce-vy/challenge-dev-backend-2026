package com.tenpo.challenge.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.tenpo.challenge.application.port.out.RetryStrategy;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

class RetryConfigTest {

  @Test
  void createsRetryStrategyFromGenericProperties() {
    RetryProperties properties =
        new RetryProperties("configured-client", 4, Duration.ofMillis(25), 3.0);

    RetryStrategy strategy = new RetryConfig().retryStrategy(properties);

    AtomicInteger attempts = new AtomicInteger();
    strategy
        .apply(
            Mono.fromCallable(
                () -> {
                  int attempt = attempts.incrementAndGet();
                  if (attempt < 4) {
                    throw new WebClientResponseException(500, "transient", null, null, null);
                  }
                  return "recovered";
                }))
        .block();

    assertThat(attempts).hasValue(4);
  }

  @Test
  void createsPercentageRetryStrategyFromProperties() {
    PercentageProviderProperties properties =
        new PercentageProviderProperties(
            new PercentageProviderProperties.Http("https://example.test", "/percentage"),
            new RetryProperties("percentage-client", 3, Duration.ofMillis(100), 2.0));

    RetryStrategy strategy = new RetryConfig().percentageRetryStrategy(properties);

    AtomicInteger attempts = new AtomicInteger();
    strategy
        .apply(
            Mono.fromCallable(
                () -> {
                  int attempt = attempts.incrementAndGet();
                  if (attempt < 3) {
                    throw new WebClientResponseException(500, "transient", null, null, null);
                  }
                  return "recovered";
                }))
        .block();

    assertThat(attempts).hasValue(3);
  }
}
