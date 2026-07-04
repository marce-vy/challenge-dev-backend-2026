package com.tenpo.challenge.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tenpo.challenge.application.PercentageProviderUnavailableException;
import com.tenpo.challenge.application.port.out.RetryStrategy;
import com.tenpo.challenge.external.percentage.ExternalPercentageProvider;
import com.tenpo.challenge.external.percentage.PercentageFetcher;
import java.math.BigDecimal;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class ExternalPercentageProviderTest {

  private final WebClient webClient =
      WebClient.builder()
          .exchangeFunction(new MockPercentageExchangeFunction())
          .baseUrl("https://percentage-provider.local")
          .build();

  @Test
  void returnsPercentageFromWebClient() {
    PercentageFetcher fetcher =
        new RetryingPercentageFetcher(webClient, singleAttemptRetry(), "/percentage");
    ExternalPercentageProvider provider = new ExternalPercentageProvider(fetcher);

    BigDecimal percentage = provider.getPercentage().block();

    assertThat(percentage).isEqualByComparingTo("10");
  }

  @Test
  void translatesWebClientFailureToApplicationException() {
    WebClient failingClient =
        WebClient.builder()
            .exchangeFunction(
                request -> Mono.error(new java.io.IOException("provider unavailable")))
            .baseUrl("https://percentage-provider.local")
            .build();
    PercentageFetcher fetcher =
        new RetryingPercentageFetcher(failingClient, singleAttemptRetry(), "/percentage");
    ExternalPercentageProvider provider = new ExternalPercentageProvider(fetcher);

    assertThatThrownBy(() -> provider.getPercentage().block())
        .isInstanceOf(PercentageProviderUnavailableException.class)
        .hasMessage("Percentage provider is unavailable");
  }

  private static RetryStrategy singleAttemptRetry() {
    return new ExponentialBackoffRetryStrategy(
        "test-percentage-provider", 1, Duration.ofMillis(1), 1.0);
  }
}
