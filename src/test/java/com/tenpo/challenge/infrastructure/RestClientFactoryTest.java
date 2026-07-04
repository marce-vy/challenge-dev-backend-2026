package com.tenpo.challenge.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.tenpo.challenge.external.percentage.PercentageResponse;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RestClientFactoryTest {

  @Test
  void createsSpringRestClientUsingMockTransport() {
    org.springframework.web.client.RestClient springRestClient =
        RestClientFactory.springRestClient(
            "https://percentage-provider.local", new MockPercentageHttpTransport());

    PercentageResponse response =
        springRestClient.get().uri("/percentage").retrieve().body(PercentageResponse.class);

    assertThat(response.percentage()).isEqualByComparingTo(new BigDecimal("10"));
  }
}
