package com.iluwatar.ratelimiter.infrastructure.datastructures;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
public class LockFreeLeakyBucket {
  private final AtomicInteger waterLevel = new AtomicInteger(0);
  private final AtomicReference<Instant> lastDripTime = new AtomicReference<>(Instant.now());
  @Getter
  private final int capacity;

  public int getAndAddWaterLevel(int delta) {
    return waterLevel.getAndAdd(delta);
  }

  public int getWaterLevel() {
    return waterLevel.get();
  }

  public void setWaterLevel(int level) {
    waterLevel.set(level);
  }

  public Instant getLastDripTime() {
    return lastDripTime.get();
  }

  public void setLastDripTime(Instant newTime) {
    lastDripTime.set(newTime);
  }
}