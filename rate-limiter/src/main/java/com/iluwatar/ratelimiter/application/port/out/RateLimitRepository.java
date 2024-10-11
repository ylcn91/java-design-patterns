package com.iluwatar.ratelimiter.application.port.out;

import com.iluwatar.ratelimiter.domain.RateLimit;

public interface RateLimitRepository {
  RateLimit getRateLimit(String clientId, String endpoint);
  void saveRateLimit(String clientId, String endpoint, RateLimit rateLimit);
}
