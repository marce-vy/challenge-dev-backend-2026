package com.tenpo.challenge.application.ratelimit;

import java.util.Objects;

public record RateLimitKey(String value) {

  public RateLimitKey {
    Objects.requireNonNull(value, "value is required");
    if (value.isBlank()) {
      throw new IllegalArgumentException("value must not be blank");
    }
  }
}
