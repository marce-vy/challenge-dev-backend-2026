package com.tenpo.challenge.application.port.out;

import com.tenpo.challenge.application.ratelimit.RateLimitPolicy;

public interface RateLimitPolicyResolver {

  RateLimitPolicy resolve(String requestURI);
}
