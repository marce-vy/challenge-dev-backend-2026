package com.tenpo.challenge.application.port.out;

import com.tenpo.challenge.application.ratelimit.RateLimitKey;

public interface RateLimitKeyResolver {

  RateLimitKey resolve(String remoteAddr, String xForwardedForHeader);
}
