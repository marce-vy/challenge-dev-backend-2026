package com.tenpo.challenge.infrastructure.ratelimit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenpo.challenge.api.dto.ErrorResponse;
import com.tenpo.challenge.application.port.in.CheckRateLimitUseCase;
import com.tenpo.challenge.application.port.out.RateLimitKeyResolver;
import com.tenpo.challenge.application.port.out.RateLimitPolicyResolver;
import com.tenpo.challenge.application.ratelimit.RateLimitDecision;
import com.tenpo.challenge.application.ratelimit.RateLimitKey;
import com.tenpo.challenge.application.ratelimit.RateLimitPolicy;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitWebFilter implements WebFilter {

  static final String LIMIT_HEADER = "X-RateLimit-Limit";
  static final String REMAINING_HEADER = "X-RateLimit-Remaining";
  static final String RETRY_AFTER_HEADER = "Retry-After";

  private final CheckRateLimitUseCase checkRateLimitUseCase;
  private final RateLimitKeyResolver keyResolver;
  private final RateLimitPolicyResolver policyResolver;
  private final ObjectMapper objectMapper;

  public RateLimitWebFilter(
      CheckRateLimitUseCase checkRateLimitUseCase,
      RateLimitKeyResolver keyResolver,
      RateLimitPolicyResolver policyResolver,
      ObjectMapper objectMapper) {
    this.checkRateLimitUseCase =
        Objects.requireNonNull(checkRateLimitUseCase, "checkRateLimitUseCase is required");
    this.keyResolver = Objects.requireNonNull(keyResolver, "keyResolver is required");
    this.policyResolver = Objects.requireNonNull(policyResolver, "policyResolver is required");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper is required");
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    String remoteAddr =
        Optional.ofNullable(request.getRemoteAddress())
            .map(InetSocketAddress::getHostString)
            .orElse("unknown");
    String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
    RateLimitKey ipKey = keyResolver.resolve(remoteAddr, forwardedFor);
    RateLimitPolicy policy = policyResolver.resolve(request.getURI().getPath());
    RateLimitKey key = new RateLimitKey(ipKey.value() + ":" + policyKey(policy));
    RateLimitDecision decision = checkRateLimitUseCase.check(key, policy);

    addRateLimitHeaders(exchange, policy, decision);
    if (decision.allowed()) {
      return chain.filter(exchange);
    }

    return writeRejectedResponse(exchange, decision);
  }

  private void addRateLimitHeaders(
      ServerWebExchange exchange, RateLimitPolicy policy, RateLimitDecision decision) {
    exchange.getResponse().getHeaders().set(LIMIT_HEADER, String.valueOf(policy.capacity()));
    exchange
        .getResponse()
        .getHeaders()
        .set(REMAINING_HEADER, String.valueOf(decision.remainingTokens()));
  }

  private Mono<Void> writeRejectedResponse(ServerWebExchange exchange, RateLimitDecision decision) {
    HttpStatus status = HttpStatus.TOO_MANY_REQUESTS;
    exchange.getResponse().setStatusCode(status);
    exchange
        .getResponse()
        .getHeaders()
        .set(RETRY_AFTER_HEADER, String.valueOf(toRetryAfterSeconds(decision.retryAfter())));
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    try {
      byte[] body =
          objectMapper.writeValueAsBytes(
              new ErrorResponse(status.value(), status.getReasonPhrase(), "Rate limit exceeded"));
      DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
      return exchange.getResponse().writeWith(Mono.just(buffer));
    } catch (JsonProcessingException e) {
      return Mono.error(e);
    }
  }

  private long toRetryAfterSeconds(Duration retryAfter) {
    return Math.max(1, (long) Math.ceil(retryAfter.toNanos() / 1_000_000_000.0));
  }

  private String policyKey(RateLimitPolicy policy) {
    return policy.capacity() + ":" + policy.refillTokens() + ":" + policy.refillPeriod();
  }
}
