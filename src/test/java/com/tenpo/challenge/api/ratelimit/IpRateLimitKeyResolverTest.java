package com.tenpo.challenge.api.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

class IpRateLimitKeyResolverTest {

  private final RateLimitKeyResolver resolver =
      new IpRateLimitKeyResolver(new ForwardedForClientIpResolver());

  @Test
  void resolvesKeyFromRemoteAddress() {
    var request =
        MockServerHttpRequest.get("/")
            .remoteAddress(InetSocketAddress.createUnresolved("127.0.0.1", 8080))
            .build();

    assertThat(resolver.resolve(request).value()).isEqualTo("127.0.0.1");
  }

  @Test
  void resolvesKeyFromForwardedForHeader() {
    var request =
        MockServerHttpRequest.get("/")
            .remoteAddress(InetSocketAddress.createUnresolved("10.0.0.10", 8080))
            .header("X-Forwarded-For", "203.0.113.1")
            .build();

    assertThat(resolver.resolve(request).value()).isEqualTo("203.0.113.1");
  }

  @Test
  void usesFirstIpFromForwardedForChain() {
    var request =
        MockServerHttpRequest.get("/")
            .remoteAddress(InetSocketAddress.createUnresolved("10.0.0.10", 8080))
            .header("X-Forwarded-For", "203.0.113.1, 10.0.0.1, 192.168.1.1")
            .build();

    assertThat(resolver.resolve(request).value()).isEqualTo("203.0.113.1");
  }

  @Test
  void fallsBackToRemoteAddrWhenForwardedForIsBlank() {
    var request =
        MockServerHttpRequest.get("/")
            .remoteAddress(InetSocketAddress.createUnresolved("10.0.0.10", 8080))
            .header("X-Forwarded-For", "  ")
            .build();

    assertThat(resolver.resolve(request).value()).isEqualTo("10.0.0.10");
  }
}
