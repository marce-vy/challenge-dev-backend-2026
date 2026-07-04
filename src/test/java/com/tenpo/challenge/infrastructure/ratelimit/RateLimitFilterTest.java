package com.tenpo.challenge.infrastructure.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenpo.challenge.application.port.in.CheckRateLimitUseCase;
import com.tenpo.challenge.application.ratelimit.RateLimitDecision;
import com.tenpo.challenge.application.ratelimit.RateLimitKey;
import com.tenpo.challenge.application.ratelimit.RateLimitPolicy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

  private static final RateLimitKey KEY = new RateLimitKey("127.0.0.1");
  private static final RateLimitPolicy POLICY = new RateLimitPolicy(3, 3, Duration.ofMinutes(1));

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void acceptedRequestIncludesRateLimitHeadersAndContinuesFilterChain() throws Exception {
    RateLimitFilter filter = filterWithDecision(RateLimitDecision.allowed(2));
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("3");
    assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("2");
    assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    assertThat(chain.getRequest()).isSameAs(request);
  }

  @Test
  void fourthRequestReturnsTooManyRequestsAndDoesNotContinueFilterChain() throws Exception {
    AtomicInteger attempts = new AtomicInteger();
    RateLimitFilter filter =
        filterWithUseCase(
            (key, policy) ->
                attempts.incrementAndGet() <= 3
                    ? RateLimitDecision.allowed(POLICY.capacity() - attempts.get())
                    : RateLimitDecision.rejected(Duration.ofSeconds(30)));
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse fourthResponse = new MockHttpServletResponse();
    MockFilterChain fourthChain = new MockFilterChain();

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    filter.doFilter(request, fourthResponse, fourthChain);

    assertThat(fourthResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    assertThat(fourthResponse.getHeader("Retry-After")).isEqualTo("30");
    assertThat(fourthResponse.getHeader("X-RateLimit-Limit")).isEqualTo("3");
    assertThat(fourthResponse.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
    assertThat(fourthResponse.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
    JsonNode body = objectMapper.readTree(fourthResponse.getContentAsString());
    assertThat(body.get("status").asInt()).isEqualTo(429);
    assertThat(body.get("error").asText()).isEqualTo("Too Many Requests");
    assertThat(body.get("message").asText()).isEqualTo("Rate limit exceeded");
    assertThat(fourthChain.getRequest()).isNull();
  }

  @Test
  void usesPolicyScopedKeyInsteadOfPathInCompositeKey() throws Exception {
    List<String> seenKeys = new ArrayList<>();
    RateLimitFilter filter =
        filterWithUseCase(
            (key, policy) -> {
              seenKeys.add(key.value());
              return RateLimitDecision.allowed(2);
            });
    MockHttpServletRequest calculationRequest =
        new MockHttpServletRequest("POST", "/api/v1/calculations");
    MockHttpServletRequest historyRequest =
        new MockHttpServletRequest("GET", "/api/v1/call-history");

    filter.doFilter(calculationRequest, new MockHttpServletResponse(), new MockFilterChain());
    filter.doFilter(historyRequest, new MockHttpServletResponse(), new MockFilterChain());

    assertThat(seenKeys).containsExactly("127.0.0.1:3:3:PT1M", "127.0.0.1:3:3:PT1M");
  }

  private RateLimitFilter filterWithDecision(RateLimitDecision decision) {
    return filterWithUseCase((key, policy) -> decision);
  }

  private RateLimitFilter filterWithUseCase(CheckRateLimitUseCase useCase) {
    return new RateLimitFilter(useCase, (addr, forwarded) -> KEY, uri -> POLICY, objectMapper);
  }
}
