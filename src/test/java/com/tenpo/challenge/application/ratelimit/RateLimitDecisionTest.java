package com.tenpo.challenge.application.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class RateLimitDecisionTest {

  @Test
  void createsAllowedDecision() {
    RateLimitDecision decision = RateLimitDecision.allowed(2);

    assertThat(decision.allowed()).isTrue();
    assertThat(decision.remainingTokens()).isEqualTo(2);
    assertThat(decision.retryAfter()).isNull();
  }

  @Test
  void createsRejectedDecision() {
    RateLimitDecision decision = RateLimitDecision.rejected(Duration.ofSeconds(15));

    assertThat(decision.allowed()).isFalse();
    assertThat(decision.remainingTokens()).isZero();
    assertThat(decision.retryAfter()).isEqualTo(Duration.ofSeconds(15));
  }

  @Test
  void rejectsNegativeRemainingTokens() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> RateLimitDecision.allowed(-1))
        .withMessage("remainingTokens must not be negative");
  }

  @Test
  void rejectsNullRetryAfterForRejectedDecision() {
    assertThatNullPointerException()
        .isThrownBy(() -> RateLimitDecision.rejected(null))
        .withMessage("retryAfter is required");
  }

  @Test
  void rejectsNonPositiveRetryAfterForRejectedDecision() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> RateLimitDecision.rejected(Duration.ZERO))
        .withMessage("retryAfter must be greater than zero");
  }
}
