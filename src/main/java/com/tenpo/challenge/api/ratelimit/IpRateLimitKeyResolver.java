package com.tenpo.challenge.api.ratelimit;

import com.tenpo.challenge.application.ratelimit.RateLimitKey;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;

public class IpRateLimitKeyResolver implements RateLimitKeyResolver {

  private final ClientIpResolver clientIpResolver;

  public IpRateLimitKeyResolver(ClientIpResolver clientIpResolver) {
    this.clientIpResolver = Objects.requireNonNull(clientIpResolver, "clientIpResolver is required");
  }

  @Override
  public RateLimitKey resolve(HttpServletRequest request) {
    Objects.requireNonNull(request, "request is required");
    return new RateLimitKey(clientIpResolver.resolve(request));
  }
}
