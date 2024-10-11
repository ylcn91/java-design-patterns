package com.iluwatar.ratelimiter.infrastructure.algorithms;

import com.iluwatar.ratelimiter.domain.RateLimit;
import com.iluwatar.ratelimiter.domain.RateLimitResult;
import com.iluwatar.ratelimiter.infrastructure.RateLimitCircuitBreaker;
import com.iluwatar.ratelimiter.infrastructure.configs.ratelimit.RateLimitConfig;
import com.iluwatar.ratelimiter.infrastructure.datastructures.LockFreeFixedWindowCounter;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FixedWindowRateLimiter extends AbstractRateLimitAlgorithm {
  final ConcurrentHashMap<String, LockFreeFixedWindowCounter> windowData = new ConcurrentHashMap<>();
  private final long timeWindowInMillis;

  @Autowired
  public FixedWindowRateLimiter(@NonNull RateLimitConfig config, @NonNull RateLimitCircuitBreaker circuitBreaker) {
    super(config.getSlidingWindowSize(), config.getBucketCapacity(), config.getRefillRate(), circuitBreaker);
    this.timeWindowInMillis = Objects.requireNonNull(config, "RateLimitConfig must not be null").getTimeWindowInMillis();
    LOGGER.info("FixedWindowRateLimiter initialized with timeWindowInMillis: {}", this.timeWindowInMillis);
  }

  @Override
  protected RateLimitResult doRateLimitCheck(RateLimit rateLimit) {
    validateRateLimit(rateLimit);
    String clientId = Objects.requireNonNull(rateLimit.client().id(), "Client ID must not be null");
    long now = System.currentTimeMillis();
    LOGGER.debug("Checking rate limit for client: {} at time: {}", clientId, now);
    LockFreeFixedWindowCounter data = windowData.computeIfAbsent(clientId, k -> new LockFreeFixedWindowCounter());
    return handleFixedWindowRateLimit(clientId, now, rateLimit, data);
  }

  private RateLimitResult handleFixedWindowRateLimit(String clientId, long now, RateLimit rateLimit, LockFreeFixedWindowCounter data) {
    long windowStart = data.getWindowStartTime();
    if (now - windowStart >= timeWindowInMillis) {
      if (data.compareAndSetWindowStartTime(windowStart, now)) {
        data.resetCount();
        LOGGER.info("Reset window for client: {}", clientId);
      }
      // Whether this thread reset the window or another did, we allow this request
      LOGGER.info("Request allowed for client: {}. New window started.", clientId);
      circuitBreaker.recordSuccess();
      return new RateLimitResult(true, Duration.ZERO);
    }

    long newCount = data.incrementAndGetCount();
    if (newCount <= rateLimit.policy().requestsPerSecond()) {
      LOGGER.info("Request allowed for client: {}. New count: {}", clientId, newCount);
      circuitBreaker.recordSuccess();
      return new RateLimitResult(true, Duration.ZERO);
    } else {
      data.decrementCount();
      Duration retryAfter = calculateRetryAfter(windowStart, timeWindowInMillis);
      LOGGER.info("Rate limit exceeded for client: {}. Count: {}. Retry after: {}", clientId, newCount - 1, retryAfter);
      circuitBreaker.recordFailure();
      return new RateLimitResult(false, retryAfter);
    }
  }

  @Override
  protected RateLimitResult doOptimisticRateLimitCheck(RateLimit rateLimit, ClientData clientData) {
    return doRateLimitCheck(rateLimit);
  }
}