package com.tenpo.challenge.api.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.tenpo.challenge.infrastructure.ForwardedForClientIpResolver;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class IpRateLimitKeyResolverTest {

  private final ClientIpResolver clientIpResolver = new ForwardedForClientIpResolver();

  @Test
  void resolvesKeyFromRemoteAddress() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("127.0.0.1");

    IpRateLimitKeyResolver resolver = new IpRateLimitKeyResolver(clientIpResolver);

    assertThat(resolver.resolve(request).value()).isEqualTo("127.0.0.1");
  }

  @Test
  void resolvesKeyFromForwardedForHeader() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("10.0.0.10");
    request.addHeader("X-Forwarded-For", "203.0.113.1");

    IpRateLimitKeyResolver resolver = new IpRateLimitKeyResolver(clientIpResolver);

    assertThat(resolver.resolve(request).value()).isEqualTo("203.0.113.1");
  }

  @Test
  void usesFirstIpFromForwardedForChain() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("10.0.0.10");
    request.addHeader("X-Forwarded-For", "203.0.113.1, 10.0.0.1, 192.168.1.1");

    IpRateLimitKeyResolver resolver = new IpRateLimitKeyResolver(clientIpResolver);

    assertThat(resolver.resolve(request).value()).isEqualTo("203.0.113.1");
  }

  @Test
  void fallsBackToRemoteAddrWhenForwardedForIsBlank() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("10.0.0.10");
    request.addHeader("X-Forwarded-For", "  ");

    IpRateLimitKeyResolver resolver = new IpRateLimitKeyResolver(clientIpResolver);

    assertThat(resolver.resolve(request).value()).isEqualTo("10.0.0.10");
  }
}
