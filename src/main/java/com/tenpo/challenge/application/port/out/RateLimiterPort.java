package com.tenpo.challenge.application.port.out;

import com.tenpo.challenge.application.ratelimit.RateLimitDecision;
import com.tenpo.challenge.application.ratelimit.RateLimitKey;
import com.tenpo.challenge.application.ratelimit.RateLimitPolicy;

public interface RateLimiterPort {

  RateLimitDecision consume(RateLimitKey key, RateLimitPolicy policy);
}
