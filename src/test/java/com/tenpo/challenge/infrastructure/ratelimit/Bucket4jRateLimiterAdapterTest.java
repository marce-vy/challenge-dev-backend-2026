package com.tenpo.challenge.infrastructure.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.tenpo.challenge.application.ratelimit.RateLimitDecision;
import com.tenpo.challenge.application.ratelimit.RateLimitKey;
import com.tenpo.challenge.application.ratelimit.RateLimitPolicy;
import io.github.bucket4j.TimeMeter;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class Bucket4jRateLimiterAdapterTest {

  private final RateLimitPolicy policy = new RateLimitPolicy(3, 3, Duration.ofMinutes(1));
  private final RateLimitKey key = new RateLimitKey("127.0.0.1");

  @Test
  void allowsFirstThreeRequestsAndRejectsFourthForSameKey() {
    Bucket4jRateLimiterAdapter adapter = new Bucket4jRateLimiterAdapter();

    RateLimitDecision first = adapter.consume(key, policy);
    RateLimitDecision second = adapter.consume(key, policy);
    RateLimitDecision third = adapter.consume(key, policy);
    RateLimitDecision fourth = adapter.consume(key, policy);

    assertThat(first.allowed()).isTrue();
    assertThat(first.remainingTokens()).isEqualTo(2);
    assertThat(second.allowed()).isTrue();
    assertThat(second.remainingTokens()).isEqualTo(1);
    assertThat(third.allowed()).isTrue();
    assertThat(third.remainingTokens()).isZero();
    assertThat(fourth.allowed()).isFalse();
    assertThat(fourth.remainingTokens()).isZero();
    assertThat(fourth.retryAfter()).isPositive();
  }

  @Test
  void keepsIndependentBudgetsForDifferentKeys() {
    Bucket4jRateLimiterAdapter adapter = new Bucket4jRateLimiterAdapter();
    RateLimitKey otherKey = new RateLimitKey("192.0.2.10");

    adapter.consume(key, policy);
    adapter.consume(key, policy);
    adapter.consume(key, policy);
    RateLimitDecision exhaustedKeyDecision = adapter.consume(key, policy);
    RateLimitDecision otherKeyDecision = adapter.consume(otherKey, policy);

    assertThat(exhaustedKeyDecision.allowed()).isFalse();
    assertThat(otherKeyDecision.allowed()).isTrue();
    assertThat(otherKeyDecision.remainingTokens()).isEqualTo(2);
  }

  @Test
  void acceptsAgainAfterConfiguredRefillPeriod() {
    MutableTimeMeter timeMeter = new MutableTimeMeter();
    Bucket4jRateLimiterAdapter adapter = new Bucket4jRateLimiterAdapter(timeMeter);

    adapter.consume(key, policy);
    adapter.consume(key, policy);
    adapter.consume(key, policy);
    RateLimitDecision rejected = adapter.consume(key, policy);

    timeMeter.advance(policy.refillPeriod());
    RateLimitDecision afterRefill = adapter.consume(key, policy);

    assertThat(rejected.allowed()).isFalse();
    assertThat(afterRefill.allowed()).isTrue();
    assertThat(afterRefill.remainingTokens()).isEqualTo(2);
  }

  @Test
  void doesNotAllowMoreThanCapacityUnderConcurrentConsumption() throws InterruptedException {
    Bucket4jRateLimiterAdapter adapter = new Bucket4jRateLimiterAdapter();
    int attempts = 24;
    AtomicInteger allowed = new AtomicInteger();
    CountDownLatch ready = new CountDownLatch(attempts);
    CountDownLatch start = new CountDownLatch(1);

    try (var executor = Executors.newFixedThreadPool(attempts)) {
      for (int attempt = 0; attempt < attempts; attempt++) {
        executor.execute(
            () -> {
              ready.countDown();
              await(start);
              if (adapter.consume(key, policy).allowed()) {
                allowed.incrementAndGet();
              }
            });
      }

      assertThat(ready.await(2, TimeUnit.SECONDS)).isTrue();
      start.countDown();
    }

    assertThat(allowed).hasValue(3);
  }

  private static void await(CountDownLatch latch) {
    try {
      latch.await();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for concurrent test", exception);
    }
  }

  private static final class MutableTimeMeter implements TimeMeter {

    private long currentTimeNanos;

    void advance(Duration duration) {
      currentTimeNanos += duration.toNanos();
    }

    @Override
    public long currentTimeNanos() {
      return currentTimeNanos;
    }

    @Override
    public boolean isWallClockBased() {
      return false;
    }
  }
}
