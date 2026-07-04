package com.tenpo.challenge.infrastructure.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tenpo.challenge.application.port.in.CheckRateLimitUseCase;
import com.tenpo.challenge.application.port.out.ClientIpResolver;
import com.tenpo.challenge.application.port.out.RateLimitKeyResolver;
import com.tenpo.challenge.application.port.out.RateLimitPolicyResolver;
import com.tenpo.challenge.application.port.out.RateLimiterPort;
import com.tenpo.challenge.application.ratelimit.RateLimitKey;
import com.tenpo.challenge.application.service.CheckRateLimitService;
import com.tenpo.challenge.infrastructure.ForwardedForClientIpResolver;
import io.github.bucket4j.Bucket;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfiguration {

  @Bean
  public Cache<RateLimitKey, Bucket> rateLimitBucketCache(RateLimitProperties properties) {
    return Caffeine.newBuilder().expireAfterAccess(properties.cacheTtl()).build();
  }

  @Bean
  public RateLimiterPort rateLimiterPort(Cache<RateLimitKey, Bucket> rateLimitBucketCache) {
    return new Bucket4jRateLimiterAdapter(rateLimitBucketCache);
  }

  @Bean
  public CheckRateLimitUseCase checkRateLimitUseCase(RateLimiterPort rateLimiterPort) {
    return new CheckRateLimitService(rateLimiterPort);
  }

  @Bean
  public ClientIpResolver clientIpResolver() {
    return new ForwardedForClientIpResolver();
  }

  @Bean
  public RateLimitKeyResolver rateLimitKeyResolver(ClientIpResolver clientIpResolver) {
    return new IpRateLimitKeyResolver(clientIpResolver);
  }

  @Bean
  public RateLimitPolicyResolver rateLimitPolicyResolver(RateLimitProperties properties) {
    return new PathBasedRateLimitPolicyResolver(
        properties.toPathPolicies(), properties.toPolicy());
  }

  @Bean
  public RateLimitFilter rateLimitFilter(
      CheckRateLimitUseCase checkRateLimitUseCase,
      RateLimitKeyResolver keyResolver,
      RateLimitPolicyResolver policyResolver,
      ObjectMapper objectMapper) {
    return new RateLimitFilter(checkRateLimitUseCase, keyResolver, policyResolver, objectMapper);
  }
}
