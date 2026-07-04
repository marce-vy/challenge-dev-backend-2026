package com.tenpo.challenge.infrastructure.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tenpo.challenge.application.port.out.RateLimiterPort;
import com.tenpo.challenge.application.ratelimit.RateLimitDecision;
import com.tenpo.challenge.application.ratelimit.RateLimitKey;
import com.tenpo.challenge.application.ratelimit.RateLimitPolicy;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.TimeMeter;
import java.time.Duration;
import java.util.Objects;

public class Bucket4jRateLimiterAdapter implements RateLimiterPort {

  private static final Duration DEFAULT_TTL = Duration.ofMinutes(2);

  private final TimeMeter timeMeter;
  private final Cache<RateLimitKey, Bucket> buckets;

  public Bucket4jRateLimiterAdapter(Cache<RateLimitKey, Bucket> buckets) {
    this(TimeMeter.SYSTEM_NANOTIME, buckets);
  }

  Bucket4jRateLimiterAdapter() {
    this(defaultCache());
  }

  Bucket4jRateLimiterAdapter(TimeMeter timeMeter) {
    this(timeMeter, defaultCache());
  }

  Bucket4jRateLimiterAdapter(TimeMeter timeMeter, Cache<RateLimitKey, Bucket> buckets) {
    this.timeMeter = Objects.requireNonNull(timeMeter, "timeMeter is required");
    this.buckets = Objects.requireNonNull(buckets, "buckets is required");
  }

  private static Cache<RateLimitKey, Bucket> defaultCache() {
    return Caffeine.newBuilder().expireAfterAccess(DEFAULT_TTL).build();
  }

  @Override
  public RateLimitDecision consume(RateLimitKey key, RateLimitPolicy policy) {
    Objects.requireNonNull(key, "key is required");
    Objects.requireNonNull(policy, "policy is required");

    Bucket bucket = buckets.get(key, ignored -> createBucket(policy));
    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

    if (probe.isConsumed()) {
      return RateLimitDecision.allowed(probe.getRemainingTokens());
    }
    return RateLimitDecision.rejected(toRetryAfter(probe.getNanosToWaitForRefill()));
  }

  private Bucket createBucket(RateLimitPolicy policy) {
    return Bucket.builder()
        .withCustomTimePrecision(timeMeter)
        .addLimit(
            limit ->
                limit
                    .capacity(policy.capacity())
                    .refillGreedy(policy.refillTokens(), policy.refillPeriod()))
        .build();
  }

  private Duration toRetryAfter(long nanosToWaitForRefill) {
    return Duration.ofNanos(Math.max(1, nanosToWaitForRefill));
  }
}
