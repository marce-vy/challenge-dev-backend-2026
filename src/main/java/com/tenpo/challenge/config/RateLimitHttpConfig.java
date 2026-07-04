package com.tenpo.challenge.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenpo.challenge.api.ratelimit.ClientIpResolver;
import com.tenpo.challenge.api.ratelimit.ForwardedForClientIpResolver;
import com.tenpo.challenge.api.ratelimit.IpRateLimitKeyResolver;
import com.tenpo.challenge.api.ratelimit.RateLimitKeyResolver;
import com.tenpo.challenge.api.ratelimit.RateLimitWebFilter;
import com.tenpo.challenge.application.port.in.CheckRateLimitUseCase;
import com.tenpo.challenge.application.port.out.RateLimitPolicyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitHttpConfig {

  @Bean
  public ClientIpResolver clientIpResolver() {
    return new ForwardedForClientIpResolver();
  }

  @Bean
  public RateLimitKeyResolver rateLimitKeyResolver(ClientIpResolver clientIpResolver) {
    return new IpRateLimitKeyResolver(clientIpResolver);
  }

  @Bean
  public RateLimitWebFilter rateLimitWebFilter(
      CheckRateLimitUseCase checkRateLimitUseCase,
      RateLimitKeyResolver keyResolver,
      RateLimitPolicyResolver policyResolver,
      ObjectMapper objectMapper) {
    return new RateLimitWebFilter(checkRateLimitUseCase, keyResolver, policyResolver, objectMapper);
  }
}
