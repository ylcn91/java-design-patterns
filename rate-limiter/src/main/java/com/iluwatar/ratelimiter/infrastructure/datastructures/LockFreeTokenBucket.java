package com.iluwatar.ratelimiter.infrastructure.datastructures;

import java.util.concurrent.atomic.AtomicLong;

public class LockFreeTokenBucket {
  private final AtomicLong tokens;
  private final AtomicLong lastRefillTime;

  public LockFreeTokenBucket(long initialTokens) {
    this.tokens = new AtomicLong(initialTokens);
    this.lastRefillTime = new AtomicLong(System.currentTimeMillis());
  }

  public long addAndGet(long delta) {
    return tokens.addAndGet(delta);
  }

  public boolean tryConsume() {
    while (true) {
      long currentTokens = tokens.get();
      if (currentTokens <= 0) return false;
      if (tokens.compareAndSet(currentTokens, currentTokens - 1)) return true;
    }
  }

  public void getAndUpdateLastRefillTime(long newRefillTime) {
    lastRefillTime.getAndSet(newRefillTime);
  }

  public long getLastRefillTime() {
    return lastRefillTime.get();
  }
}