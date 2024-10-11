package com.iluwatar.ratelimiter.application.port.in;

import com.iluwatar.ratelimiter.domain.RateLimitResult;

public interface RateLimitUseCase {
  RateLimitResult checkRateLimit(String endpoint, String clientId);
}
