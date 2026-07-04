package com.tenpo.challenge.api.ratelimit;

import org.springframework.http.server.reactive.ServerHttpRequest;

public class ForwardedForClientIpResolver implements ClientIpResolver {

  @Override
  public String resolve(ServerHttpRequest request) {
    String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddress() != null
        ? request.getRemoteAddress().getHostString()
        : "unknown";
  }
}
