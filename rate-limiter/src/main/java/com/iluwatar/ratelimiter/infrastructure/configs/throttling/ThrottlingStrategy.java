package com.iluwatar.ratelimiter.infrastructure.configs.throttling;

import com.iluwatar.ratelimiter.domain.RateLimitResult;

public interface ThrottlingStrategy {
  void throttle(RateLimitResult result) throws InterruptedException;
}
