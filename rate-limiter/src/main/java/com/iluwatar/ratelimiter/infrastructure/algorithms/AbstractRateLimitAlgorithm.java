package com.iluwatar.ratelimiter.infrastructure.algorithms;

import com.iluwatar.ratelimiter.domain.RateLimit;
import com.iluwatar.ratelimiter.domain.RateLimitResult;
import com.iluwatar.ratelimiter.infrastructure.LockManager;
import com.iluwatar.ratelimiter.infrastructure.RateLimitCircuitBreaker;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.StampedLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
public abstract class AbstractRateLimitAlgorithm implements RateLimitAlgorithm {

  protected final long slidingWindowSize;
  protected final long bucketCapacity;
  protected final long refillRate;
  protected final RateLimitCircuitBreaker circuitBreaker;
  private final ConcurrentMap<String, ClientData> clientDataMap = new ConcurrentHashMap<>();

  protected static class ClientData {
    final StampedLock lock = new StampedLock();
    final AtomicLong lastAccessTime = new AtomicLong(System.currentTimeMillis());
  }

  protected AbstractRateLimitAlgorithm(long slidingWindowSize, long bucketCapacity, long refillRate, RateLimitCircuitBreaker circuitBreaker) {
    this.slidingWindowSize = slidingWindowSize;
    this.bucketCapacity = bucketCapacity;
    this.refillRate = refillRate;
    this.circuitBreaker = circuitBreaker;
  }

  @Override
  public RateLimitResult checkRateLimit(RateLimit rateLimit) {
    LOGGER.info("Checking rate limit for client: {}", rateLimit.client().id());

    if (!circuitBreaker.isCircuitClosed()) {
      Duration retryAfter = circuitBreaker.getResetTimeout();
      LOGGER.warn("Circuit breaker is open. Retry after: {}", retryAfter);
      return new RateLimitResult(false, retryAfter);
    }

    String clientId = rateLimit.client().id();
    ClientData clientData = clientDataMap.computeIfAbsent(clientId, k -> new ClientData());
    clientData.lastAccessTime.set(System.currentTimeMillis());

    LOGGER.info("Using optimistic read lock for client: {}", clientId);
    RateLimitResult result = LockManager.withOptimisticRead(clientData.lock,
        () -> doOptimisticRateLimitCheck(rateLimit, clientData),
        () -> doRateLimitCheckWithLogging(rateLimit, clientId, clientData));

    if (result == null) {
      LOGGER.info("Optimistic check failed, falling back to full write lock check for client: {}", clientId);
      result = LockManager.withWriteLock(clientData.lock, () -> doRateLimitCheckWithLogging(rateLimit, clientId, clientData));
    }

    return result;
  }

  protected abstract RateLimitResult doRateLimitCheck(RateLimit rateLimit);

  protected abstract RateLimitResult doOptimisticRateLimitCheck(RateLimit rateLimit, ClientData clientData);

  protected void validateRateLimit(RateLimit rateLimit) {
    if (rateLimit == null || rateLimit.client() == null || rateLimit.policy() == null) {
      throw new IllegalArgumentException("RateLimit, RateLimit.client, and RateLimit.policy must not be null");
    }
  }

  protected StampedLock getClientLock(String clientId) {
    return clientDataMap.computeIfAbsent(clientId, k -> new ClientData()).lock;
  }

  @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
  protected void cleanupInactiveClients() {
    long currentTime = System.currentTimeMillis();
    long inactivityThreshold = TimeUnit.HOURS.toMillis(1);  // Consider clients inactive after 1 hour

    clientDataMap.entrySet().removeIf(entry -> {
      long lastAccessTime = entry.getValue().lastAccessTime.get();
      return (currentTime - lastAccessTime) > inactivityThreshold;
    });

    LOGGER.info("Cleaned up inactive clients. Remaining active clients: {}", clientDataMap.size());
  }

  protected Duration calculateRetryAfter(long startTime, long timeWindowInMillis) {
    long now = System.currentTimeMillis();
    long timePassedInWindow = now - startTime;
    long timeLeftInWindow = Math.max(0, timeWindowInMillis - timePassedInWindow);
    return Duration.ofMillis(timeLeftInWindow);
  }

  private RateLimitResult doRateLimitCheckWithLogging(RateLimit rateLimit, String clientId, ClientData clientData) {
    LOGGER.info("Performing rate limit check with write lock for client: {}", clientId);
    RateLimitResult result = doRateLimitCheck(rateLimit);
    LOGGER.info("Rate limit check result for client {}: {}", clientId, result);
    return result;
  }
}
