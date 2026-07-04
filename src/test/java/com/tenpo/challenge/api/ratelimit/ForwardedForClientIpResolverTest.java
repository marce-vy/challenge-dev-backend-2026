package com.tenpo.challenge.api.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ForwardedForClientIpResolverTest {

  private final ClientIpResolver resolver = new ForwardedForClientIpResolver();

  @Test
  void returnsRemoteAddrWhenNoForwardedHeader() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("192.168.1.1");

    assertThat(resolver.resolve(request)).isEqualTo("192.168.1.1");
  }

  @Test
  void returnsFirstForwardedForIp() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("10.0.0.1");
    request.addHeader("X-Forwarded-For", "203.0.113.42");

    assertThat(resolver.resolve(request)).isEqualTo("203.0.113.42");
  }

  @Test
  void returnsFirstIpFromForwardedForChain() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("10.0.0.1");
    request.addHeader("X-Forwarded-For", "203.0.113.1, 10.0.0.2, 192.168.0.1");

    assertThat(resolver.resolve(request)).isEqualTo("203.0.113.1");
  }

  @Test
  void fallsBackWhenForwardedForIsBlank() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("10.0.0.1");
    request.addHeader("X-Forwarded-For", "  ");

    assertThat(resolver.resolve(request)).isEqualTo("10.0.0.1");
  }
}
