package com.tenpo.challenge.infrastructure.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.tenpo.challenge.application.port.out.ClientIpResolver;
import org.junit.jupiter.api.Test;

class IpRateLimitKeyResolverTest {

  private final ClientIpResolver clientIpResolver =
      new com.tenpo.challenge.infrastructure.ForwardedForClientIpResolver();

  @Test
  void resolvesKeyFromRemoteAddress() {
    IpRateLimitKeyResolver resolver = new IpRateLimitKeyResolver(clientIpResolver);

    assertThat(resolver.resolve("127.0.0.1", null).value()).isEqualTo("127.0.0.1");
  }

  @Test
  void resolvesKeyFromForwardedForHeader() {
    IpRateLimitKeyResolver resolver = new IpRateLimitKeyResolver(clientIpResolver);

    assertThat(resolver.resolve("10.0.0.10", "203.0.113.1").value()).isEqualTo("203.0.113.1");
  }

  @Test
  void usesFirstIpFromForwardedForChain() {
    IpRateLimitKeyResolver resolver = new IpRateLimitKeyResolver(clientIpResolver);

    assertThat(resolver.resolve("10.0.0.10", "203.0.113.1, 10.0.0.1, 192.168.1.1").value())
        .isEqualTo("203.0.113.1");
  }

  @Test
  void fallsBackToRemoteAddrWhenForwardedForIsBlank() {
    IpRateLimitKeyResolver resolver = new IpRateLimitKeyResolver(clientIpResolver);

    assertThat(resolver.resolve("10.0.0.10", "  ").value()).isEqualTo("10.0.0.10");
  }
}
