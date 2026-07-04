package com.tenpo.challenge.infrastructure;

import com.tenpo.challenge.application.port.out.RetryStrategy;
import java.io.IOException;
import java.time.Duration;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

public class ExponentialBackoffRetryStrategy implements RetryStrategy {

  private final int maxAttempts;
  private final Duration initialBackoff;
  private final double multiplier;

  public ExponentialBackoffRetryStrategy(
      String name, int maxAttempts, Duration initialBackoff, double multiplier) {
    this.maxAttempts = maxAttempts;
    this.initialBackoff = initialBackoff;
    this.multiplier = multiplier;
  }

  @Override
  public <T> Mono<T> apply(Mono<T> source) {
    return source.retryWhen(
        Retry.backoff(maxAttempts - 1, initialBackoff)
            .maxBackoff(initialBackoff.multipliedBy((long) Math.pow(multiplier, maxAttempts)))
            .filter(
                throwable ->
                    throwable instanceof IOException
                        || throwable instanceof WebClientResponseException));
  }
}
