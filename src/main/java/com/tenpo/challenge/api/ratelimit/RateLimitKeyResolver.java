package com.tenpo.challenge.api.ratelimit;

import com.tenpo.challenge.application.ratelimit.RateLimitKey;
import jakarta.servlet.http.HttpServletRequest;

public interface RateLimitKeyResolver {

  RateLimitKey resolve(HttpServletRequest request);
}
