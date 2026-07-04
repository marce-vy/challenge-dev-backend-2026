package com.tenpo.challenge.application.port.in;

import com.tenpo.challenge.application.ratelimit.RateLimitDecision;
import com.tenpo.challenge.application.ratelimit.RateLimitKey;
import com.tenpo.challenge.application.ratelimit.RateLimitPolicy;

public interface CheckRateLimitUseCase {

  RateLimitDecision check(RateLimitKey key, RateLimitPolicy policy);
}
