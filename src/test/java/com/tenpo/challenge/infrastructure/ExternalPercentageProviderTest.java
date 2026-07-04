package com.tenpo.challenge.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tenpo.challenge.application.exception.PercentageProviderUnavailableException;
import com.tenpo.challenge.external.percentage.ExternalPercentageProvider;
import com.tenpo.challenge.external.percentage.PercentageFetcher;
import com.tenpo.challenge.infrastructure.RetryingPercentageFetcher;
import io.github.resilience4j.retry.Retry;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class ExternalPercentageProviderTest {

  @Test
  void returnsPercentageFromRestClient() {
    RestClient restClient =
        RestClientFactory.springRestClient(
            "https://percentage-provider.local", new MockPercentageHttpTransport());

    PercentageFetcher fetcher =
        new RetryingPercentageFetcher(restClient, singleAttemptRetry(), "/percentage");
    ExternalPercentageProvider provider = new ExternalPercentageProvider(fetcher);

    BigDecimal percentage = provider.getPercentage();

    assertThat(percentage).isEqualByComparingTo("10");
  }

  @Test
  void translatesRestClientFailureToApplicationException() {
    RestClient restClient =
        RestClientFactory.springRestClient(
            "https://percentage-provider.local",
            (uri, httpMethod) -> {
              throw new java.io.IOException("provider unavailable");
            });

    PercentageFetcher fetcher =
        new RetryingPercentageFetcher(restClient, singleAttemptRetry(), "/percentage");
    ExternalPercentageProvider provider = new ExternalPercentageProvider(fetcher);

    assertThatThrownBy(provider::getPercentage)
        .isInstanceOf(PercentageProviderUnavailableException.class)
        .hasMessage("Percentage provider is unavailable");
  }

  private static Retry singleAttemptRetry() {
    io.github.resilience4j.retry.RetryConfig retryConfig =
        io.github.resilience4j.retry.RetryConfig.custom().maxAttempts(1).build();
    return Retry.of("test-percentage-provider", retryConfig);
  }
}
