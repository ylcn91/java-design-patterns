package com.iluwatar.ratelimiter.infrastructure.throttling;

import com.iluwatar.ratelimiter.domain.RateLimitResult;
import com.iluwatar.ratelimiter.infrastructure.configs.throttling.ThrottlingStrategy;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DelayThrottlingStrategy implements ThrottlingStrategy {

  // Prevent possible deadlock
  private final ScheduledExecutorService delayExecutor = Executors.newScheduledThreadPool(10);

  @Override
  public void throttle(RateLimitResult result) {
    LOGGER.info("Throttling request");
    if (!result.allowed()) {
      LOGGER.info("Rate limit exceeded. Retrying after {} milliseconds", result.retryAfter().toMillis());
      CompletableFuture<Void> delayFuture = new CompletableFuture<>();
      delayExecutor.schedule(() -> delayFuture.complete(null), result.retryAfter().toMillis(), TimeUnit.MILLISECONDS);
      delayFuture.join();
    }
  }
}
