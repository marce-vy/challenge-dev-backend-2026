package com.tenpo.challenge.api.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

class ForwardedForClientIpResolverTest {

  private final ClientIpResolver resolver = new ForwardedForClientIpResolver();

  @Test
  void returnsRemoteAddrWhenNoForwardedHeader() {
    var request =
        MockServerHttpRequest.get("/")
            .remoteAddress(InetSocketAddress.createUnresolved("192.168.1.1", 8080))
            .build();

    assertThat(resolver.resolve(request)).isEqualTo("192.168.1.1");
  }

  @Test
  void returnsFirstForwardedForIp() {
    var request =
        MockServerHttpRequest.get("/")
            .remoteAddress(InetSocketAddress.createUnresolved("10.0.0.1", 8080))
            .header("X-Forwarded-For", "203.0.113.42")
            .build();

    assertThat(resolver.resolve(request)).isEqualTo("203.0.113.42");
  }

  @Test
  void returnsFirstIpFromForwardedForChain() {
    var request =
        MockServerHttpRequest.get("/")
            .remoteAddress(InetSocketAddress.createUnresolved("10.0.0.1", 8080))
            .header("X-Forwarded-For", "203.0.113.1, 10.0.0.2, 192.168.0.1")
            .build();

    assertThat(resolver.resolve(request)).isEqualTo("203.0.113.1");
  }

  @Test
  void fallsBackWhenForwardedForIsBlank() {
    var request =
        MockServerHttpRequest.get("/")
            .remoteAddress(InetSocketAddress.createUnresolved("10.0.0.1", 8080))
            .header("X-Forwarded-For", "  ")
            .build();

    assertThat(resolver.resolve(request)).isEqualTo("10.0.0.1");
  }
}
