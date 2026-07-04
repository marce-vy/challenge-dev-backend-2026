package com.tenpo.challenge.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tenpo.challenge.application.PercentageProviderUnavailableException;
import com.tenpo.challenge.external.percentage.ExternalPercentageProvider;
import com.tenpo.challenge.external.percentage.PercentageFetcher;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class ExternalPercentageProviderRetryTest {

  @Test
  void retriesTransientFailuresBeforeReturningResponse() {
    AtomicInteger attempts = new AtomicInteger();
    WebClient client =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  if (attempts.incrementAndGet() == 1) {
                    return Mono.error(new IOException("temporary provider failure"));
                  }
                  return new MockPercentageExchangeFunction().exchange(request);
                })
            .baseUrl("https://percentage-provider.local")
            .build();
    ExternalPercentageProvider provider = provider(client);

    assertThat(provider.getPercentage().block()).isEqualByComparingTo(new BigDecimal("10"));
    assertThat(attempts).hasValue(2);
  }

  @Test
  void retriesExactlyThreeTotalAttemptsBeforeFailing() {
    AtomicInteger attempts = new AtomicInteger();
    WebClient client =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  attempts.incrementAndGet();
                  return Mono.error(new IOException("provider unavailable"));
                })
            .baseUrl("https://percentage-provider.local")
            .build();
    ExternalPercentageProvider provider = provider(client);

    assertThatThrownBy(() -> provider.getPercentage().block())
        .isInstanceOf(PercentageProviderUnavailableException.class);
    assertThat(attempts).hasValue(3);
  }

  private static ExternalPercentageProvider provider(WebClient client) {
    PercentageFetcher fetcher =
        new RetryingPercentageFetcher(
            client,
            new ExponentialBackoffRetryStrategy("test", 3, java.time.Duration.ofMillis(1), 1.0),
            "/percentage");
    return new ExternalPercentageProvider(fetcher);
  }
}
