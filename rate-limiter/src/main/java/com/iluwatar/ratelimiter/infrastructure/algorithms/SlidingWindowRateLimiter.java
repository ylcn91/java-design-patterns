package com.iluwatar.ratelimiter.infrastructure.algorithms;

import com.iluwatar.ratelimiter.domain.RateLimit;
import com.iluwatar.ratelimiter.domain.RateLimitResult;
import com.iluwatar.ratelimiter.infrastructure.datastructures.LockFreeTimestampQueue;
import com.iluwatar.ratelimiter.infrastructure.RateLimitCircuitBreaker;
import com.iluwatar.ratelimiter.infrastructure.configs.ratelimit.RateLimitConfig;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
public class SlidingWindowRateLimiter extends AbstractRateLimitAlgorithm {
  private final ConcurrentMap<String, LockFreeTimestampQueue> requestTimestamps = new ConcurrentHashMap<>();
  private final long timeWindowInMillis;

  @Autowired
  public SlidingWindowRateLimiter(@NonNull RateLimitConfig config, @NonNull RateLimitCircuitBreaker circuitBreaker) {
    super(config.getSlidingWindowSize(), config.getBucketCapacity(), config.getRefillRate(), circuitBreaker);
    this.timeWindowInMillis = config.getTimeWindowInMillis();
    LOGGER.info("SlidingWindowRateLimiter initialized with timeWindowInMillis: {}", this.timeWindowInMillis);
  }

  @Override
  protected RateLimitResult doRateLimitCheck(RateLimit rateLimit) {
    validateRateLimit(rateLimit);

    LOGGER.info("Checking rate limit for client: {} at time: {}", rateLimit.client().id(), System.currentTimeMillis());
    String clientId = Objects.requireNonNull(rateLimit.client().id(), "Client ID must not be null");
    long now = System.currentTimeMillis();
    LockFreeTimestampQueue timestamps = getRequestTimestamps(clientId);
    return handleSlidingWindowRateLimit(clientId, now, rateLimit, timestamps);
  }

  @Override
  protected RateLimitResult doOptimisticRateLimitCheck(RateLimit rateLimit, ClientData clientData) {
    return doRateLimitCheck(rateLimit);
  }

  private LockFreeTimestampQueue getRequestTimestamps(String clientId) {
    return requestTimestamps.computeIfAbsent(clientId, k -> new LockFreeTimestampQueue());
  }

  private RateLimitResult handleSlidingWindowRateLimit(String clientId, long now, RateLimit rateLimit, LockFreeTimestampQueue timestamps) {
    cleanupOldRequests(now, timestamps);
    if (isRequestAllowed(rateLimit, timestamps)) {
      timestamps.offer(now);
      LOGGER.debug("Request granted for client: {}. Requests in window: {}", clientId, timestamps.size());
      circuitBreaker.recordSuccess();
      return new RateLimitResult(true, Duration.ZERO);
    } else {
      Duration retryAfter = timeUntilReset(timestamps, now);
      LOGGER.warn("Rate limit exceeded for client: {}. Retry after: {}", clientId, retryAfter);
      circuitBreaker.recordFailure();
      return new RateLimitResult(false, retryAfter);
    }
  }

  private void cleanupOldRequests(long now, LockFreeTimestampQueue timestamps) {
    long windowStartTime = now - timeWindowInMillis;
    long polledTimestamp;
    while ((polledTimestamp = timestamps.poll(windowStartTime)) != -1) {
      LOGGER.trace("Removed old timestamp: {} for window start time: {}", polledTimestamp, windowStartTime);
    }
  }

  private boolean isRequestAllowed(RateLimit rateLimit, LockFreeTimestampQueue timestamps) {
    return timestamps.size() < rateLimit.policy().requestsPerSecond();
  }

  private Duration timeUntilReset(LockFreeTimestampQueue timestamps, long now) {
    long firstRequestTime = timestamps.peekFirst();
    if (firstRequestTime == -1) {
      return Duration.ZERO;
    }
    long timeUntilResetMillis = timeWindowInMillis - (now - firstRequestTime);
    return Duration.ofMillis(Math.max(0, timeUntilResetMillis));
  }
}