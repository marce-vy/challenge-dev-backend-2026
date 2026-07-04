package com.tenpo.challenge.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.tenpo.challenge.application.port.out.ClientIpResolver;
import org.junit.jupiter.api.Test;

class ForwardedForClientIpResolverTest {

  private final ClientIpResolver resolver = new ForwardedForClientIpResolver();

  @Test
  void returnsRemoteAddrWhenNoForwardedHeader() {
    assertThat(resolver.resolve("192.168.1.1", null)).isEqualTo("192.168.1.1");
  }

  @Test
  void returnsFirstForwardedForIp() {
    assertThat(resolver.resolve("10.0.0.1", "203.0.113.42")).isEqualTo("203.0.113.42");
  }

  @Test
  void returnsFirstIpFromForwardedForChain() {
    assertThat(resolver.resolve("10.0.0.1", "203.0.113.1, 10.0.0.2, 192.168.0.1"))
        .isEqualTo("203.0.113.1");
  }

  @Test
  void fallsBackWhenForwardedForIsBlank() {
    assertThat(resolver.resolve("10.0.0.1", "  ")).isEqualTo("10.0.0.1");
  }
}
