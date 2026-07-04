package com.tenpo.challenge.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

class CallHistoryAsyncPropertiesTest {

  @Test
  void bindsConfiguredConcurrencyLimit() {
    MockEnvironment environment =
        new MockEnvironment().withProperty("call-history.async.concurrency-limit", "4");

    CallHistoryAsyncProperties properties =
        Binder.get(environment)
            .bind("call-history.async", Bindable.of(CallHistoryAsyncProperties.class))
            .get();

    assertThat(properties.concurrencyLimit()).isEqualTo(4);
  }

  @Test
  void bindsDefaultConfiguredConcurrencyLimit() {
    CallHistoryAsyncProperties properties =
        Binder.get(new MockEnvironment().withProperty("call-history.async.concurrency-limit", "2"))
            .bindOrCreate("call-history.async", Bindable.of(CallHistoryAsyncProperties.class));

    assertThat(properties.concurrencyLimit()).isEqualTo(2);
  }

  @Test
  void rejectsNonPositiveConcurrencyLimit() {
    assertThatThrownBy(() -> new CallHistoryAsyncProperties(0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("greater than zero");
  }
}
