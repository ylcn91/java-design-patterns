package com.iluwatar.ratelimiter.infrastructure;

import com.iluwatar.ratelimiter.domain.RateLimit;
import com.iluwatar.ratelimiter.domain.RateLimitResult;

public interface RateLimitAlgorithm {
  RateLimitResult checkRateLimit(RateLimit rateLimit);
}

