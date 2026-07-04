package com.tenpo.challenge.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

class CallHistoryAsyncExecutorConfigTest {

  @Test
  void configuresDedicatedVirtualThreadExecutorWithExplicitConcurrencyLimit()
      throws InterruptedException {
    CallHistoryConfig config = new CallHistoryConfig();

    SimpleAsyncTaskExecutor executor =
        config.callHistoryExecutor(new CallHistoryAsyncProperties(3));

    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Thread> threadRef = new AtomicReference<>();
    executor.execute(
        () -> {
          threadRef.set(Thread.currentThread());
          latch.countDown();
        });

    assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
    assertThat(executor.getConcurrencyLimit()).isEqualTo(3);
    assertThat(threadRef.get()).isNotNull();
    assertThat(threadRef.get().isVirtual()).isTrue();
  }
}
