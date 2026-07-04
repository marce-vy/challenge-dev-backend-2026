package com.tenpo.challenge.application.service;

import com.tenpo.challenge.application.port.in.CheckRateLimitUseCase;
import com.tenpo.challenge.application.port.out.RateLimiterPort;
import com.tenpo.challenge.application.ratelimit.RateLimitDecision;
import com.tenpo.challenge.application.ratelimit.RateLimitKey;
import com.tenpo.challenge.application.ratelimit.RateLimitPolicy;
import java.util.Objects;

public class CheckRateLimitService implements CheckRateLimitUseCase {

  private final RateLimiterPort rateLimiterPort;

  public CheckRateLimitService(RateLimiterPort rateLimiterPort) {
    this.rateLimiterPort = Objects.requireNonNull(rateLimiterPort, "rateLimiterPort is required");
  }

  @Override
  public RateLimitDecision check(RateLimitKey key, RateLimitPolicy policy) {
    Objects.requireNonNull(key, "key is required");
    Objects.requireNonNull(policy, "policy is required");
    return rateLimiterPort.consume(key, policy);
  }
}
