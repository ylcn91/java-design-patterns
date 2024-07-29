package com.iluwatar.ratelimiter.domain;

import jakarta.validation.constraints.Min;

public record Policy(@Min(1) int requestsPerSecond) {
  public static final Policy NO_LIMIT = new Policy(Integer.MAX_VALUE);
}
