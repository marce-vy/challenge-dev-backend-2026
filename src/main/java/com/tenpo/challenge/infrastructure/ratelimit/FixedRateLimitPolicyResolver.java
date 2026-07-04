package com.tenpo.challenge.infrastructure.ratelimit;

import com.tenpo.challenge.application.port.out.RateLimitPolicyResolver;
import com.tenpo.challenge.application.ratelimit.RateLimitPolicy;
import java.util.Objects;

public class FixedRateLimitPolicyResolver implements RateLimitPolicyResolver {

  private final RateLimitPolicy policy;

  public FixedRateLimitPolicyResolver(RateLimitPolicy policy) {
    this.policy = Objects.requireNonNull(policy, "policy is required");
  }

  @Override
  public RateLimitPolicy resolve(String requestURI) {
    return policy;
  }
}
