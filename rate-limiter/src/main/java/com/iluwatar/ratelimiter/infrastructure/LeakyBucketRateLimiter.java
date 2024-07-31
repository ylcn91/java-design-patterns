package com.iluwatar.ratelimiter.infrastructure;

import com.iluwatar.ratelimiter.domain.RateLimit;
import com.iluwatar.ratelimiter.domain.RateLimitResult;
import com.iluwatar.ratelimiter.infrastructure.datastructures.LockFreeLeakyBucket;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class LeakyBucketRateLimiter extends AbstractRateLimitAlgorithm {

  private final ConcurrentHashMap<String, LockFreeLeakyBucket> buckets = new ConcurrentHashMap<>();
  private final int bucketCapacity;

  // TODO: fix the constructor to use the RateLimitConfig and RateLimitCircuitBreaker
  public LeakyBucketRateLimiter(@NonNull RateLimitConfig config, @NonNull RateLimitCircuitBreaker circuitBreaker) {
    super(config.getSlidingWindowSize(), config.getBucketCapacity(), config.getRefillRate(), circuitBreaker);
    this.bucketCapacity = (int) config.getBucketCapacity();
    LOGGER.info("LeakyBucketRateLimiter initialized with timeWindowInMillis: {} and bucketCapacity: {}",
        config.getTimeWindowInMillis(), this.bucketCapacity);
  }

  @Override
  protected RateLimitResult doRateLimitCheck(RateLimit rateLimit) {
    validateRateLimit(rateLimit);
    String clientId = Objects.requireNonNull(rateLimit.client().id(), "Client ID must not be null");
    Instant now = Instant.now();
    return handleLeakyBucketRateLimit(clientId, now, rateLimit);
  }

  @Override
  protected RateLimitResult doOptimisticRateLimitCheck(RateLimit rateLimit, ClientData clientData) {
    // Optimistic check should not modify the state and must quickly assess if the request can be allowed
    LockFreeLeakyBucket bucket = buckets.get(rateLimit.client().id());
    if (bucket == null) {
      return new RateLimitResult(true, Duration.ZERO);
    }
    Instant now = Instant.now();
    long millisElapsed = Duration.between(bucket.getLastDripTime(), now).toMillis();
    double leakRatePerMillisecond = rateLimit.policy().requestsPerSecond() / 1000.0;
    int drippedWater = (int) Math.ceil(millisElapsed * leakRatePerMillisecond);
    int currentWaterLevel = Math.max(0, bucket.getWaterLevel() - drippedWater);

    if (currentWaterLevel < bucketCapacity) {
      return new RateLimitResult(true, Duration.ZERO);
    } else {
      Duration retryAfter = timeUntilReset(bucket, rateLimit.policy().requestsPerSecond());
      return new RateLimitResult(false, retryAfter);
    }
  }

  private RateLimitResult handleLeakyBucketRateLimit(String clientId, Instant now, RateLimit rateLimit) {
    LockFreeLeakyBucket bucket = buckets.computeIfAbsent(clientId, k -> new LockFreeLeakyBucket(bucketCapacity));
    long millisElapsed = Duration.between(bucket.getLastDripTime(), now).toMillis();
    double requestsPerSecond = rateLimit.policy().requestsPerSecond();
    double leakRatePerMillisecond = requestsPerSecond / 1000.0;
    int drippedWater = (int) Math.ceil(millisElapsed * leakRatePerMillisecond);

    int currentWaterLevel = Math.max(0, bucket.getWaterLevel() - drippedWater);
    if (currentWaterLevel < bucketCapacity) {
      currentWaterLevel++;
      bucket.setWaterLevel(currentWaterLevel);
      bucket.setLastDripTime(now);
      circuitBreaker.recordSuccess();
      return new RateLimitResult(true, Duration.ZERO);
    } else {
      Duration retryAfter = timeUntilReset(bucket, requestsPerSecond);
      circuitBreaker.recordFailure();
      return new RateLimitResult(false, retryAfter);
    }
  }

  private Duration timeUntilReset(LockFreeLeakyBucket bucket, double requestsPerSecond) {
    double excessWater = bucket.getWaterLevel() - bucketCapacity + 1;
    double leakRatePerMillisecond = requestsPerSecond / 1000.0;
    long timeToEmptyMs = (long) Math.ceil(excessWater / leakRatePerMillisecond);
    return Duration.ofMillis(timeToEmptyMs);
  }
}
