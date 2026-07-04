package com.tenpo.challenge.api.ratelimit;

import com.tenpo.challenge.application.ratelimit.RateLimitKey;
import org.springframework.http.server.reactive.ServerHttpRequest;

public interface RateLimitKeyResolver {

  RateLimitKey resolve(ServerHttpRequest request);
}
