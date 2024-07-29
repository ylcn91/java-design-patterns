package com.iluwatar.ratelimiter.domain;

public interface RateLimitExemptionManager {
  void addExemption(String clientId);
  void removeExemption(String clientId);
}
