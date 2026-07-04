package com.tenpo.challenge.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tenpo.challenge.application.port.out.RetryStrategy;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

class ExponentialBackoffRetryStrategyTest {

  @Test
  void retriesUpToMaxAttempts() {
    RetryStrategy strategy =
        new ExponentialBackoffRetryStrategy("test", 3, Duration.ofMillis(10), 2.0);

    AtomicInteger attempts = new AtomicInteger();
    String result =
        strategy
            .apply(
                Mono.fromCallable(
                    () -> {
                      int attempt = attempts.incrementAndGet();
                      if (attempt < 3) {
                        throw new WebClientResponseException(500, "transient", null, null, null);
                      }
                      return "success";
                    }))
            .block();

    assertThat(attempts).hasValue(3);
    assertThat(result).isEqualTo("success");
  }

  @Test
  void doesNotRetryNonTransientExceptions() {
    RetryStrategy strategy =
        new ExponentialBackoffRetryStrategy("test", 3, Duration.ofMillis(10), 2.0);

    AtomicInteger attempts = new AtomicInteger();
    assertThatThrownBy(
            () ->
                strategy
                    .apply(
                        Mono.fromCallable(
                            () -> {
                              attempts.incrementAndGet();
                              throw new IllegalArgumentException("invalid");
                            }))
                    .block())
        .isInstanceOf(IllegalArgumentException.class);

    assertThat(attempts).hasValue(1);
  }
}
