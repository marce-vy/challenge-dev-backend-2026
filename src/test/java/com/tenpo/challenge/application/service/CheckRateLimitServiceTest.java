package com.tenpo.challenge.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.tenpo.challenge.application.ratelimit.RateLimitDecision;
import com.tenpo.challenge.application.ratelimit.RateLimitKey;
import com.tenpo.challenge.application.ratelimit.RateLimitPolicy;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class CheckRateLimitServiceTest {

  @Test
  void returnsAllowedDecisionFromPort() {
    RateLimitKey key = new RateLimitKey("127.0.0.1");
    RateLimitPolicy policy = new RateLimitPolicy(3, 3, Duration.ofMinutes(1));
    AtomicReference<RateLimitKey> consumedKey = new AtomicReference<>();
    AtomicReference<RateLimitPolicy> consumedPolicy = new AtomicReference<>();
    CheckRateLimitService useCase =
        new CheckRateLimitService(
            (receivedKey, receivedPolicy) -> {
              consumedKey.set(receivedKey);
              consumedPolicy.set(receivedPolicy);
              return RateLimitDecision.allowed(2);
            });

    RateLimitDecision decision = useCase.check(key, policy);

    assertThat(decision.allowed()).isTrue();
    assertThat(decision.remainingTokens()).isEqualTo(2);
    assertThat(consumedKey).hasValue(key);
    assertThat(consumedPolicy).hasValue(policy);
  }

  @Test
  void returnsRejectedDecisionFromPort() {
    RateLimitKey key = new RateLimitKey("127.0.0.1");
    RateLimitPolicy policy = new RateLimitPolicy(3, 3, Duration.ofMinutes(1));
    CheckRateLimitService useCase =
        new CheckRateLimitService(
            (receivedKey, receivedPolicy) -> RateLimitDecision.rejected(Duration.ofSeconds(30)));

    RateLimitDecision decision = useCase.check(key, policy);

    assertThat(decision.allowed()).isFalse();
    assertThat(decision.remainingTokens()).isZero();
    assertThat(decision.retryAfter()).isEqualTo(Duration.ofSeconds(30));
  }

  @Test
  void rejectsMissingDependenciesAndInputs() {
    RateLimitKey key = new RateLimitKey("127.0.0.1");
    RateLimitPolicy policy = new RateLimitPolicy(3, 3, Duration.ofMinutes(1));
    CheckRateLimitService useCase =
        new CheckRateLimitService((receivedKey, receivedPolicy) -> RateLimitDecision.allowed(2));

    assertThatNullPointerException()
        .isThrownBy(() -> new CheckRateLimitService(null))
        .withMessage("rateLimiterPort is required");
    assertThatNullPointerException()
        .isThrownBy(() -> useCase.check(null, policy))
        .withMessage("key is required");
    assertThatNullPointerException()
        .isThrownBy(() -> useCase.check(key, null))
        .withMessage("policy is required");
  }
}
