package com.tenpo.challenge.infrastructure;

import com.tenpo.challenge.application.port.out.RetryStrategy;
import com.tenpo.challenge.external.percentage.PercentageFetcher;
import com.tenpo.challenge.external.percentage.PercentageResponse;
import java.util.Objects;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class RetryingPercentageFetcher implements PercentageFetcher {

  private final WebClient webClient;
  private final RetryStrategy retryStrategy;
  private final String percentagePath;

  public RetryingPercentageFetcher(
      WebClient webClient, RetryStrategy retryStrategy, String percentagePath) {
    this.webClient = Objects.requireNonNull(webClient, "webClient is required");
    this.retryStrategy = Objects.requireNonNull(retryStrategy, "retryStrategy is required");
    this.percentagePath = Objects.requireNonNull(percentagePath, "percentagePath is required");
  }

  @Override
  public Mono<PercentageResponse> fetch() {
    return retryStrategy.apply(
        webClient.get().uri(percentagePath).retrieve().bodyToMono(PercentageResponse.class));
  }
}
