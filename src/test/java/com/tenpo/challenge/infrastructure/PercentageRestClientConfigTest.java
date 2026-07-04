package com.tenpo.challenge.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.tenpo.challenge.config.PercentageProviderProperties;
import com.tenpo.challenge.config.PercentageRestClientConfig;
import com.tenpo.challenge.config.RetryProperties;
import com.tenpo.challenge.external.percentage.PercentageResponse;
import java.math.BigDecimal;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class PercentageRestClientConfigTest {

  @Test
  void springRestClientReceivesPercentageThroughLocalMockTransport() {
    PercentageRestClientConfig config =
        new PercentageRestClientConfig(
            new PercentageProviderProperties(
                new PercentageProviderProperties.Http(
                    "https://percentage-provider.local", "/percentage"),
                new RetryProperties("percentage-provider", 3, Duration.ofMillis(100), 2.0)));
    org.springframework.web.client.RestClient springRestClient =
        config.springPercentageRestClient(new MockPercentageHttpTransport());

    PercentageResponse response =
        springRestClient.get().uri("/percentage").retrieve().body(PercentageResponse.class);

    assertThat(response.percentage()).isEqualByComparingTo(new BigDecimal("10"));
  }
}
