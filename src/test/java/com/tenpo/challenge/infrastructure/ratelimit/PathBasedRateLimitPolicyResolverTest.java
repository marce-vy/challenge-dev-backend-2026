package com.tenpo.challenge.infrastructure.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.tenpo.challenge.application.ratelimit.RateLimitPolicy;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class PathBasedRateLimitPolicyResolverTest {

  private static final RateLimitPolicy DEFAULT = new RateLimitPolicy(3, 3, Duration.ofMinutes(1));
  private static final RateLimitPolicy SWAGGER = new RateLimitPolicy(60, 60, Duration.ofMinutes(1));

  @Test
  void resolvesDefaultPolicyForUnmatchedPath() {
    PathBasedRateLimitPolicyResolver resolver =
        new PathBasedRateLimitPolicyResolver(
            List.of(new PathBasedRateLimitPolicyResolver.PathPolicy("/swagger-ui/**", SWAGGER)),
            DEFAULT);

    RateLimitPolicy policy = resolver.resolve("/api/v1/calculations");

    assertThat(policy).isEqualTo(DEFAULT);
  }

  @Test
  void resolvesPathSpecificPolicyForMatchedPattern() {
    PathBasedRateLimitPolicyResolver resolver =
        new PathBasedRateLimitPolicyResolver(
            List.of(new PathBasedRateLimitPolicyResolver.PathPolicy("/swagger-ui/**", SWAGGER)),
            DEFAULT);

    RateLimitPolicy policy = resolver.resolve("/swagger-ui/index.html");

    assertThat(policy).isEqualTo(SWAGGER);
  }

  @Test
  void resolvesFirstMatchingPatternWhenMultipleOverlap() {
    RateLimitPolicy v3Policy = new RateLimitPolicy(30, 30, Duration.ofMinutes(1));
    PathBasedRateLimitPolicyResolver resolver =
        new PathBasedRateLimitPolicyResolver(
            List.of(
                new PathBasedRateLimitPolicyResolver.PathPolicy("/swagger-ui/**", SWAGGER),
                new PathBasedRateLimitPolicyResolver.PathPolicy("/v3/api-docs/**", v3Policy)),
            DEFAULT);

    assertThat(resolver.resolve("/swagger-ui/index.html")).isEqualTo(SWAGGER);
    assertThat(resolver.resolve("/v3/api-docs/swagger-config")).isEqualTo(v3Policy);
  }
}
