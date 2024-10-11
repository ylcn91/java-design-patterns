package com.iluwatar.ratelimiter.infrastructure.datastructures;

import java.util.concurrent.atomic.AtomicLong;

public class LockFreeFixedWindowCounter {
  private final AtomicLong requestCount = new AtomicLong(0);
  private final AtomicLong windowStartTime = new AtomicLong(System.currentTimeMillis());

  public long incrementAndGetCount() {
    return requestCount.incrementAndGet();
  }

  public void decrementCount() {
    requestCount.decrementAndGet();
  }

  public void resetCount() {
    requestCount.set(0);
  }

  public long getWindowStartTime() {
    return windowStartTime.get();
  }

  public boolean compareAndSetWindowStartTime(long expect, long update) {
    return windowStartTime.compareAndSet(expect, update);
  }

  // this method used by test cases
  public long getRequestCount() {
    return requestCount.get();
  }
}