package com.iluwatar.ratelimiter.application.port.out;

import com.iluwatar.ratelimiter.domain.Client;

public interface MetricsCollector {
  void recordRateLimitExceeded(String endpoint, Client client);
  void recordRateLimitSuccess(String endpoint, Client client);
}
