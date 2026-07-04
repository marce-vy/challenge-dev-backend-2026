package com.tenpo.challenge.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

class PercentageProviderPropertiesTest {

  @Test
  void bindsHttpAndRetryProperties() {
    MockEnvironment environment =
        new MockEnvironment()
            .withProperty("percentage-provider.http.base-url", "https://example.test")
            .withProperty("percentage-provider.http.path", "/v1/percentage")
            .withProperty("percentage-provider.retry.name", "custom-percentage")
            .withProperty("percentage-provider.retry.max-attempts", "4")
            .withProperty("percentage-provider.retry.initial-backoff", "250ms")
            .withProperty("percentage-provider.retry.backoff-multiplier", "1.5");

    PercentageProviderProperties properties =
        Binder.get(environment)
            .bind("percentage-provider", Bindable.of(PercentageProviderProperties.class))
            .get();

    assertThat(properties.http().baseUrl()).isEqualTo("https://example.test");
    assertThat(properties.http().path()).isEqualTo("/v1/percentage");
    assertThat(properties.retry()).isInstanceOf(RetryProperties.class);
    assertThat(properties.retry().name()).isEqualTo("custom-percentage");
    assertThat(properties.retry().maxAttempts()).isEqualTo(4);
    assertThat(properties.retry().initialBackoff()).isEqualTo(Duration.ofMillis(250));
    assertThat(properties.retry().backoffMultiplier()).isEqualTo(1.5);
  }
}
