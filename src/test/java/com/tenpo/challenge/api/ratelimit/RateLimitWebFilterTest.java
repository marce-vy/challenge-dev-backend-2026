package com.tenpo.challenge.api.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenpo.challenge.application.port.in.CheckRateLimitUseCase;
import com.tenpo.challenge.application.port.out.RateLimitPolicyResolver;
import com.tenpo.challenge.application.ratelimit.RateLimitDecision;
import com.tenpo.challenge.application.ratelimit.RateLimitKey;
import com.tenpo.challenge.application.ratelimit.RateLimitPolicy;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;

class RateLimitWebFilterTest {

  private static final RateLimitPolicy POLICY = new RateLimitPolicy(3, 3, Duration.ofMinutes(1));
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private static final RateLimitKeyResolver fixedKeyResolver =
      request -> new RateLimitKey("127.0.0.1");
  private static final RateLimitPolicyResolver fixedPolicyResolver = uri -> POLICY;

  @Test
  void acceptedRequestContinuesFilterChainWithHeaders() {
    WebFilterChain chain = exchange -> exchange.getResponse().setComplete();
    RateLimitWebFilter filter =
        new RateLimitWebFilter(
            (key, policy) -> RateLimitDecision.allowed(2),
            fixedKeyResolver,
            fixedPolicyResolver,
            objectMapper);

    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/calculations"));

    filter.filter(exchange, chain).block();

    assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit")).isEqualTo("3");
    assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining"))
        .isEqualTo("2");
    assertThat(exchange.getResponse().getStatusCode()).isNull();
  }

  @Test
  void rejectedRequestReturnsTooManyRequestsWithBody() {
    WebFilterChain chain = exchange -> exchange.getResponse().setComplete();
    RateLimitWebFilter filter =
        new RateLimitWebFilter(
            (key, policy) -> RateLimitDecision.rejected(Duration.ofSeconds(30)),
            fixedKeyResolver,
            fixedPolicyResolver,
            objectMapper);

    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/calculations"));

    filter.filter(exchange, chain).block();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("30");
    assertThat(exchange.getResponse().getHeaders().getContentType())
        .isEqualTo(MediaType.APPLICATION_JSON);
  }

  @Test
  void usesPolicyScopedKeyInsteadOfPathInCompositeKey() {
    List<String> seenKeys = new java.util.ArrayList<>();
    CheckRateLimitUseCase useCase =
        (key, policy) -> {
          seenKeys.add(key.value());
          return RateLimitDecision.allowed(2);
        };
    WebFilterChain chain = exchange -> exchange.getResponse().setComplete();
    RateLimitWebFilter filter =
        new RateLimitWebFilter(useCase, fixedKeyResolver, fixedPolicyResolver, objectMapper);

    MockServerWebExchange calculationExchange =
        MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/calculations"));
    MockServerWebExchange historyExchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/call-history"));

    filter.filter(calculationExchange, chain).block();
    filter.filter(historyExchange, chain).block();

    assertThat(seenKeys)
        .containsExactly("127.0.0.1:3:3:PT1M", "127.0.0.1:3:3:PT1M");
  }
}
