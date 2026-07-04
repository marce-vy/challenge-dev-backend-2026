package com.tenpo.challenge.external.percentage;

import com.tenpo.challenge.application.PercentageProviderUnavailableException;
import com.tenpo.challenge.application.port.out.PercentageProviderPort;
import java.math.BigDecimal;
import java.util.Objects;
import reactor.core.publisher.Mono;

public class ExternalPercentageProvider implements PercentageProviderPort {

  private static final String UNAVAILABLE_MESSAGE = "Percentage provider is unavailable";

  private final PercentageFetcher fetcher;

  public ExternalPercentageProvider(PercentageFetcher fetcher) {
    this.fetcher = Objects.requireNonNull(fetcher, "fetcher is required");
  }

  @Override
  public Mono<BigDecimal> getPercentage() {
    return fetcher
        .fetch()
        .flatMap(
            response -> {
              if (response == null || response.percentage() == null) {
                return Mono.error(
                    new IllegalStateException("Percentage provider returned an empty response"));
              }
              return Mono.just(response.percentage());
            })
        .onErrorMap(
            RuntimeException.class,
            e -> new PercentageProviderUnavailableException(UNAVAILABLE_MESSAGE, e));
  }
}
