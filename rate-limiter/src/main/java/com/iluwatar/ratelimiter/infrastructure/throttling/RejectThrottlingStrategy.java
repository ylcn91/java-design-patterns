package com.iluwatar.ratelimiter.infrastructure.throttling;

import com.iluwatar.ratelimiter.domain.RateLimitResult;
import com.iluwatar.ratelimiter.infrastructure.configs.throttling.ThrottlingStrategy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RejectThrottlingStrategy implements ThrottlingStrategy {

  @Override
  public void throttle(RateLimitResult result) {
    if (!result.allowed()) {
      LOGGER.info("Rate limit exceeded. Rejecting request.");
      throw new RuntimeException("Rate limit exceeded. Please try again after " + result.retryAfter().toMillis() + " milliseconds.");
    }
  }
}
