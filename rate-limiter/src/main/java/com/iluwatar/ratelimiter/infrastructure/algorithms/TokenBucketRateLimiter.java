package com.iluwatar.ratelimiter.infrastructure.algorithms;

import com.iluwatar.ratelimiter.domain.RateLimit;
import com.iluwatar.ratelimiter.domain.RateLimitResult;
import com.iluwatar.ratelimiter.infrastructure.RateLimitCircuitBreaker;
import com.iluwatar.ratelimiter.infrastructure.configs.ratelimit.RateLimitConfig;
import com.iluwatar.ratelimiter.infrastructure.datastructures.LockFreeTokenBucket;
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
public class TokenBucketRateLimiter extends AbstractRateLimitAlgorithm {
  private final ConcurrentHashMap<String, LockFreeTokenBucket> buckets = new ConcurrentHashMap<>();
  private final long timeWindowInMillis;

  @Autowired
  public TokenBucketRateLimiter(@NonNull RateLimitConfig config, @NonNull RateLimitCircuitBreaker circuitBreaker) {
    super(config.getSlidingWindowSize(), config.getBucketCapacity(), config.getRefillRate(), circuitBreaker);
    this.timeWindowInMillis = Objects.requireNonNull(config, "RateLimitConfig must not be null").getTimeWindowInMillis();
    Objects.requireNonNull(circuitBreaker, "RateLimitCircuitBreaker must not be null");
    LOGGER.info("TokenBucketRateLimiter initialized with timeWindowInMillis: {}", this.timeWindowInMillis);
  }

  @Override
  protected RateLimitResult doRateLimitCheck(RateLimit rateLimit) {
    validateRateLimit(rateLimit);

    String clientId = Objects.requireNonNull(rateLimit.client().id(), "Client ID must not be null");
    Instant now = Instant.now();

    LockFreeTokenBucket bucket = buckets.computeIfAbsent(clientId, k -> new LockFreeTokenBucket(bucketCapacity));
    return handleTokenBucketRateLimit(clientId, now, rateLimit, bucket);
  }

  @Override
  protected RateLimitResult doOptimisticRateLimitCheck(RateLimit rateLimit, ClientData clientData) {
    return doRateLimitCheck(rateLimit);
  }

  private RateLimitResult handleTokenBucketRateLimit(String clientId, Instant now, RateLimit rateLimit, LockFreeTokenBucket bucket) {
    long millisElapsed = Duration.between(Instant.ofEpochMilli(bucket.getLastRefillTime()), now).toMillis();
    long refillTokens = (millisElapsed * rateLimit.policy().requestsPerSecond()) / timeWindowInMillis;

    bucket.addAndGet(refillTokens);
    bucket.getAndUpdateLastRefillTime(now.toEpochMilli());

    if (bucket.tryConsume()) {
      LOGGER.debug("Token granted for client: {}. Tokens left: {}", clientId, bucket.addAndGet(0));
      circuitBreaker.recordSuccess();
      return new RateLimitResult(true, Duration.ZERO);
    } else {
      Duration retryAfter = timeUntilReset(bucket, rateLimit);
      LOGGER.warn("Rate limit exceeded for client: {}. Retry after: {}", clientId, retryAfter);
      circuitBreaker.recordFailure();
      return new RateLimitResult(false, retryAfter);
    }
  }

  private Duration timeUntilReset(LockFreeTokenBucket bucket, RateLimit rateLimit) {
    long tokensNeeded = 1 - bucket.addAndGet(0);
    long timeUntilResetMillis = (tokensNeeded * timeWindowInMillis) / rateLimit.policy().requestsPerSecond();
    return Duration.ofMillis(Math.max(0, timeUntilResetMillis));
  }
}