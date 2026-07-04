package com.tenpo.challenge.application.ratelimit;

import java.time.Duration;
import java.util.Objects;

public record RateLimitPolicy(int capacity, int refillTokens, Duration refillPeriod) {

  public RateLimitPolicy {
    if (capacity <= 0) {
      throw new IllegalArgumentException("capacity must be greater than zero");
    }
    if (refillTokens <= 0) {
      throw new IllegalArgumentException("refillTokens must be greater than zero");
    }
    Objects.requireNonNull(refillPeriod, "refillPeriod is required");
    if (refillPeriod.isZero() || refillPeriod.isNegative()) {
      throw new IllegalArgumentException("refillPeriod must be greater than zero");
    }
  }
}
