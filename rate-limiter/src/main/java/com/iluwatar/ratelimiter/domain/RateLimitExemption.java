package com.iluwatar.ratelimiter.domain;

public interface RateLimitExemption {
  boolean isExempt(Client client);
}
