package com.iluwatar.ratelimiter.infrastructure.throttling;

import com.iluwatar.ratelimiter.domain.RateLimitResult;
import com.iluwatar.ratelimiter.infrastructure.configs.throttling.ThrottlingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ExponentialBackoffThrottlingStrategy implements ThrottlingStrategy {

  private final long initialDelay;
  private final double multiplier;

  @Override
  public void throttle(RateLimitResult result) throws InterruptedException {
    if (!result.allowed()) {
      long retryAfterMillis = result.retryAfter().toMillis();
      long delay = Math.min((long) (initialDelay * Math.pow(multiplier, retryAfterMillis / 1000.0)), Long.MAX_VALUE);
      LOGGER.info("Rate limit exceeded. Retrying after {} milliseconds", delay);
      Thread.sleep(delay);
    }
  }
}
