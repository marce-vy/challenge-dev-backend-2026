package com.tenpo.challenge.application.ratelimit;

import java.time.Duration;
import java.util.Objects;

public record RateLimitDecision(boolean allowed, long remainingTokens, Duration retryAfter) {

  public RateLimitDecision {
    if (remainingTokens < 0) {
      throw new IllegalArgumentException("remainingTokens must not be negative");
    }
    if (!allowed) {
      Objects.requireNonNull(retryAfter, "retryAfter is required");
      if (retryAfter.isZero() || retryAfter.isNegative()) {
        throw new IllegalArgumentException("retryAfter must be greater than zero");
      }
    }
  }

  public static RateLimitDecision allowed(long remainingTokens) {
    return new RateLimitDecision(true, remainingTokens, null);
  }

  public static RateLimitDecision rejected(Duration retryAfter) {
    return new RateLimitDecision(false, 0, retryAfter);
  }
}
