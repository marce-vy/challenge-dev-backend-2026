package com.tenpo.challenge.api.ratelimit;

import com.tenpo.challenge.application.ratelimit.RateLimitKey;
import java.util.Objects;
import org.springframework.http.server.reactive.ServerHttpRequest;

public class IpRateLimitKeyResolver implements RateLimitKeyResolver {

  private final ClientIpResolver clientIpResolver;

  public IpRateLimitKeyResolver(ClientIpResolver clientIpResolver) {
    this.clientIpResolver =
        Objects.requireNonNull(clientIpResolver, "clientIpResolver is required");
  }

  @Override
  public RateLimitKey resolve(ServerHttpRequest request) {
    String ip = clientIpResolver.resolve(request);
    return new RateLimitKey(ip);
  }
}
