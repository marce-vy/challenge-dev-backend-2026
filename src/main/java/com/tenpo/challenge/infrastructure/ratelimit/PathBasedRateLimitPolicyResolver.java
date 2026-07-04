package com.tenpo.challenge.infrastructure.ratelimit;

import com.tenpo.challenge.application.port.out.RateLimitPolicyResolver;
import com.tenpo.challenge.application.ratelimit.RateLimitPolicy;
import java.util.List;
import java.util.Objects;
import org.springframework.util.AntPathMatcher;

public class PathBasedRateLimitPolicyResolver implements RateLimitPolicyResolver {

  private final List<PathPolicy> pathPolicies;
  private final RateLimitPolicy defaultPolicy;
  private final AntPathMatcher matcher = new AntPathMatcher();

  public PathBasedRateLimitPolicyResolver(
      List<PathPolicy> pathPolicies, RateLimitPolicy defaultPolicy) {
    this.pathPolicies =
        List.copyOf(Objects.requireNonNull(pathPolicies, "pathPolicies is required"));
    this.defaultPolicy = Objects.requireNonNull(defaultPolicy, "defaultPolicy is required");
  }

  @Override
  public RateLimitPolicy resolve(String requestURI) {
    return pathPolicies.stream()
        .filter(pp -> matcher.match(pp.pattern(), requestURI))
        .findFirst()
        .map(PathPolicy::policy)
        .orElse(defaultPolicy);
  }

  public record PathPolicy(String pattern, RateLimitPolicy policy) {

    public PathPolicy {
      Objects.requireNonNull(pattern, "pattern is required");
      Objects.requireNonNull(policy, "policy is required");
    }
  }
}
