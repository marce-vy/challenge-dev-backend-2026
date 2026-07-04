package com.tenpo.challenge.application.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class RateLimitPolicyTest {

  @Test
  void exposesConfiguredBudget() {
    RateLimitPolicy policy = new RateLimitPolicy(3, 3, Duration.ofMinutes(1));

    assertThat(policy.capacity()).isEqualTo(3);
    assertThat(policy.refillTokens()).isEqualTo(3);
    assertThat(policy.refillPeriod()).isEqualTo(Duration.ofMinutes(1));
  }

  @Test
  void rejectsNonPositiveCapacity() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new RateLimitPolicy(0, 3, Duration.ofMinutes(1)))
        .withMessage("capacity must be greater than zero");
  }

  @Test
  void rejectsNonPositiveRefillTokens() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new RateLimitPolicy(3, 0, Duration.ofMinutes(1)))
        .withMessage("refillTokens must be greater than zero");
  }

  @Test
  void rejectsNullRefillPeriod() {
    assertThatNullPointerException()
        .isThrownBy(() -> new RateLimitPolicy(3, 3, null))
        .withMessage("refillPeriod is required");
  }

  @Test
  void rejectsNonPositiveRefillPeriod() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new RateLimitPolicy(3, 3, Duration.ZERO))
        .withMessage("refillPeriod must be greater than zero");
  }
}
