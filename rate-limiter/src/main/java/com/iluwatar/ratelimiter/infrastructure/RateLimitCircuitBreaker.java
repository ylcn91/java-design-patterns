package com.iluwatar.ratelimiter.infrastructure;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RateLimitCircuitBreaker {

  private final AtomicInteger failureCount = new AtomicInteger(0);
  final AtomicBoolean isOpen = new AtomicBoolean(false);
  final AtomicBoolean isHalfOpen = new AtomicBoolean(false);

  @Getter
  private volatile int failureThreshold;

  @Getter
  volatile Duration resetTimeout;

  volatile Instant lastOpenTime;

  public RateLimitCircuitBreaker(int failureThreshold, Duration resetTimeout) {
    this.failureThreshold = failureThreshold;
    this.resetTimeout = resetTimeout;
  }

  public synchronized void reset() {
    log.info("Resetting circuit breaker state");
    failureCount.set(0);
    isOpen.set(false);
    isHalfOpen.set(false);
    lastOpenTime = null;
  }

  public synchronized boolean isCircuitClosed() {
    if (isOpen.get()) {
      if (isHalfOpen.get()) {
        log.debug("Circuit is in half-open state");
        return true;
      }

      log.info("Circuit is open");
      if (Duration.between(lastOpenTime, Instant.now()).compareTo(resetTimeout) > 0) {
        log.info("Transitioning to half-open state");
        isHalfOpen.set(true);
        isOpen.set(false);
        return true;
      }
      return false;
    }

    if (failureCount.get() >= failureThreshold) {
      log.info("Opening circuit");
      isOpen.set(true);
      lastOpenTime = Instant.now();
      return false;
    }
    return true;
  }

  public synchronized void recordFailure() {
    if (isHalfOpen.get()) {
      log.info("Recording failure in half-open state, opening circuit");
      isHalfOpen.set(false);
      isOpen.set(true);
      lastOpenTime = Instant.now();
      failureCount.set(0);
    } else {
      LOGGER.info("Recording failure");
      failureCount.incrementAndGet();
    }
  }

  public synchronized void recordSuccess() {
    if (isHalfOpen.get()) {
      log.info("Recording success in half-open state, closing circuit");
      isHalfOpen.set(false);
      isOpen.set(false);
      failureCount.set(0);
    } else {
      log.info("Recording success");
      failureCount.set(0);
      log.info("Failure count reset to 0");
    }
  }
}
