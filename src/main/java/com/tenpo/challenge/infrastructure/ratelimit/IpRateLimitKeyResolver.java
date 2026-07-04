package com.tenpo.challenge.infrastructure.ratelimit;

import com.tenpo.challenge.application.port.out.ClientIpResolver;
import com.tenpo.challenge.application.port.out.RateLimitKeyResolver;
import com.tenpo.challenge.application.ratelimit.RateLimitKey;
import java.util.Objects;

public class IpRateLimitKeyResolver implements RateLimitKeyResolver {

  private final ClientIpResolver clientIpResolver;

  public IpRateLimitKeyResolver(ClientIpResolver clientIpResolver) {
    this.clientIpResolver =
        Objects.requireNonNull(clientIpResolver, "clientIpResolver is required");
  }

  @Override
  public RateLimitKey resolve(String remoteAddr, String xForwardedForHeader) {
    String ip = clientIpResolver.resolve(remoteAddr, xForwardedForHeader);
    return new RateLimitKey(ip);
  }
}
