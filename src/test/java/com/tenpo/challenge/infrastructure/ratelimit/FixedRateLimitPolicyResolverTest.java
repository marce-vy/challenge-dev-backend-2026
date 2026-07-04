package com.tenpo.challenge.infrastructure.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.tenpo.challenge.application.port.out.RateLimitPolicyResolver;
import com.tenpo.challenge.application.ratelimit.RateLimitPolicy;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class FixedRateLimitPolicyResolverTest {

  @Test
  void resolvesConfiguredGlobalPolicy() {
    RateLimitPolicy configuredPolicy = new RateLimitPolicy(3, 3, Duration.ofMinutes(1));
    RateLimitPolicyResolver resolver = new FixedRateLimitPolicyResolver(configuredPolicy);

    RateLimitPolicy policy = resolver.resolve("/api/v1/calculations");

    assertThat(policy).isEqualTo(configuredPolicy);
  }

  @Test
  void rejectsNullPolicy() {
    assertThatNullPointerException()
        .isThrownBy(() -> new FixedRateLimitPolicyResolver(null))
        .withMessage("policy is required");
  }
}
