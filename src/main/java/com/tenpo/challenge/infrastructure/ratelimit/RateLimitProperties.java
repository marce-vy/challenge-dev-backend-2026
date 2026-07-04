package com.tenpo.challenge.infrastructure.ratelimit;

import com.tenpo.challenge.application.ratelimit.RateLimitPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

  @Positive private int capacity;

  @Positive private int refillTokens;

  @NotNull private Duration refillPeriod;

  @NotNull private Duration cacheTtl;

  private List<PathPolicyProperties> pathPolicies = List.of();

  public int capacity() {
    return capacity;
  }

  public void setCapacity(int capacity) {
    this.capacity = capacity;
  }

  public int refillTokens() {
    return refillTokens;
  }

  public void setRefillTokens(int refillTokens) {
    this.refillTokens = refillTokens;
  }

  public Duration refillPeriod() {
    return refillPeriod;
  }

  public void setRefillPeriod(Duration refillPeriod) {
    this.refillPeriod = refillPeriod;
  }

  public Duration cacheTtl() {
    return cacheTtl;
  }

  public void setCacheTtl(Duration cacheTtl) {
    this.cacheTtl = cacheTtl;
  }

  public List<PathPolicyProperties> pathPolicies() {
    return pathPolicies;
  }

  public void setPathPolicies(List<PathPolicyProperties> pathPolicies) {
    this.pathPolicies = pathPolicies != null ? List.copyOf(pathPolicies) : List.of();
  }

  public RateLimitPolicy toPolicy() {
    return new RateLimitPolicy(capacity, refillTokens, refillPeriod);
  }

  public List<PathBasedRateLimitPolicyResolver.PathPolicy> toPathPolicies() {
    return pathPolicies.stream().map(this::toPathPolicy).toList();
  }

  private PathBasedRateLimitPolicyResolver.PathPolicy toPathPolicy(PathPolicyProperties p) {
    return new PathBasedRateLimitPolicyResolver.PathPolicy(
        p.pattern(),
        new RateLimitPolicy(p.capacity(), p.refillTokens(), p.refillPeriod()));
  }

  public static class PathPolicyProperties {

    @NotBlank private String pattern;

    @Positive private int capacity;

    @Positive private int refillTokens;

    @NotNull private Duration refillPeriod;

    public String pattern() {
      return pattern;
    }

    public void setPattern(String pattern) {
      this.pattern = pattern;
    }

    public int capacity() {
      return capacity;
    }

    public void setCapacity(int capacity) {
      this.capacity = capacity;
    }

    public int refillTokens() {
      return refillTokens;
    }

    public void setRefillTokens(int refillTokens) {
      this.refillTokens = refillTokens;
    }

    public Duration refillPeriod() {
      return refillPeriod;
    }

    public void setRefillPeriod(Duration refillPeriod) {
      this.refillPeriod = refillPeriod;
    }
  }
}
