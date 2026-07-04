package com.tenpo.challenge.application.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class RateLimitKeyTest {

  @Test
  void exposesSourceIpValue() {
    RateLimitKey key = new RateLimitKey("127.0.0.1");

    assertThat(key.value()).isEqualTo("127.0.0.1");
  }

  @Test
  void rejectsNullValue() {
    assertThatNullPointerException()
        .isThrownBy(() -> new RateLimitKey(null))
        .withMessage("value is required");
  }

  @Test
  void rejectsBlankValue() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new RateLimitKey(" "))
        .withMessage("value must not be blank");
  }
}
