package com.iluwatar.ratelimiter.domain;

public enum AlgorithmType {
  FIXED_WINDOW,
  SLIDING_WINDOW,
  TOKEN_BUCKET,
  LEAKY_BUCKET,
  QUOTA
}
