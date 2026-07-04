package com.tenpo.challenge.api.ratelimit;

import jakarta.servlet.http.HttpServletRequest;

public class ForwardedForClientIpResolver implements ClientIpResolver {

  private static final String X_FORWARDED_FOR = "X-Forwarded-For";

  @Override
  public String resolve(HttpServletRequest request) {
    String forwarded = request.getHeader(X_FORWARDED_FOR);
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
