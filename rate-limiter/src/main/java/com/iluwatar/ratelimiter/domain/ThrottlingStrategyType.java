package com.iluwatar.ratelimiter.domain;

public enum ThrottlingStrategyType {
  DELAY,
  REJECT,
  EXPONENTIAL_BACKOFF
}
