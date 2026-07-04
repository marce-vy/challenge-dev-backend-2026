package com.tenpo.challenge.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tenpo.challenge.application.exception.PercentageProviderUnavailableException;
import com.tenpo.challenge.external.percentage.ExternalPercentageProvider;
import com.tenpo.challenge.external.percentage.PercentageFetcher;
import com.tenpo.challenge.infrastructure.RetryingPercentageFetcher;
import io.github.resilience4j.retry.Retry;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;

class ExternalPercentageProviderRetryTest {

  @Test
  void retriesTransientFailuresBeforeReturningResponse() {
    AtomicInteger attempts = new AtomicInteger();
    MockPercentageHttpTransport mockTransport = new MockPercentageHttpTransport();
    ClientHttpRequestFactory requestFactory =
        (uri, httpMethod) -> {
          if (attempts.incrementAndGet() == 1) {
            throw new IOException("temporary provider failure");
          }
          return mockTransport.createRequest(uri, httpMethod);
        };
    ExternalPercentageProvider provider = provider(requestFactory);

    BigDecimal response = provider.getPercentage();

    assertThat(response).isEqualByComparingTo(new BigDecimal("10"));
    assertThat(attempts).hasValue(2);
  }

  @Test
  void retriesExactlyThreeTotalAttemptsBeforeFailing() {
    AtomicInteger attempts = new AtomicInteger();
    ClientHttpRequestFactory requestFactory =
        new ClientHttpRequestFactory() {
          @Override
          public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod)
              throws IOException {
            attempts.incrementAndGet();
            throw new IOException("provider unavailable");
          }
        };
    ExternalPercentageProvider provider = provider(requestFactory);

    assertThatThrownBy(provider::getPercentage)
        .isInstanceOf(PercentageProviderUnavailableException.class);
    assertThat(attempts).hasValue(3);
  }

  private static ExternalPercentageProvider provider(ClientHttpRequestFactory requestFactory) {
    org.springframework.web.client.RestClient springRestClient =
        RestClientFactory.springRestClient("https://percentage-provider.local", requestFactory);
    PercentageFetcher fetcher =
        new RetryingPercentageFetcher(springRestClient, retry(), "/percentage");
    return new ExternalPercentageProvider(fetcher);
  }

  private static Retry retry() {
    io.github.resilience4j.retry.RetryConfig retryConfig =
        io.github.resilience4j.retry.RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ZERO)
            .retryExceptions(RuntimeException.class)
            .build();
    return Retry.of("test-percentage-provider", retryConfig);
  }
}
