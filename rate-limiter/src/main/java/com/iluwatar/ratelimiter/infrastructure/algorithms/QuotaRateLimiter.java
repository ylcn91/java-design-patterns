package com.iluwatar.ratelimiter.infrastructure.algorithms;

import com.iluwatar.ratelimiter.domain.RateLimit;
import com.iluwatar.ratelimiter.domain.RateLimitResult;
import com.iluwatar.ratelimiter.infrastructure.RateLimitCircuitBreaker;
import com.iluwatar.ratelimiter.infrastructure.configs.ratelimit.RateLimitConfig;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Slf4j
@Component
public class QuotaRateLimiter extends AbstractRateLimitAlgorithm {

  private final long quotaLimit;
  private final Duration quotaResetPeriod;

  @Autowired
  public QuotaRateLimiter(@NonNull RateLimitConfig config, @NonNull RateLimitCircuitBreaker circuitBreaker) {
    super(config.getSlidingWindowSize(), config.getBucketCapacity(), config.getRefillRate(), circuitBreaker);
    this.quotaLimit = Objects.requireNonNull(config, "RateLimitConfig must not be null").getQuotaLimit();
    this.quotaResetPeriod = Duration.ofDays(1); // Assuming daily reset, but this could be configurable
    LOGGER.info("QuotaRateLimiter initialized with quotaLimit: {}", this.quotaLimit);
  }

  @Override
  protected RateLimitResult doRateLimitCheck(RateLimit rateLimit) {
    validateRateLimit(rateLimit);
    String clientId = Objects.requireNonNull(rateLimit.client().id(), "Client ID must not be null");
    return handleQuotaRateLimit(clientId, rateLimit);
  }

  @Override
  protected RateLimitResult doOptimisticRateLimitCheck(RateLimit rateLimit, ClientData clientData) {
    validateRateLimit(rateLimit);
    String clientId = Objects.requireNonNull(rateLimit.client().id(), "Client ID must not be null");

    Instant now = Instant.now();
    if (Duration.between(rateLimit.lastResetTime(), now).compareTo(quotaResetPeriod) >= 0) {
      // If quota reset period has elapsed, we can't make an optimistic decision
      return null; // This will trigger a fallback to the write lock
    }

    if (rateLimit.quotaUsed() >= quotaLimit) {
      Duration retryAfter = Duration.between(now, rateLimit.lastResetTime().plus(quotaResetPeriod));
      LOGGER.warn("Optimistic check: Quota limit exceeded for client: {}. Retry after: {}", clientId, retryAfter);
      circuitBreaker.recordFailure();
      return new RateLimitResult(false, retryAfter);
    }

    LOGGER.debug("Optimistic check: Request allowed for client: {}. Current quota used: {}", clientId, rateLimit.quotaUsed());
    return new RateLimitResult(true, Duration.ZERO);
  }

  private RateLimitResult handleQuotaRateLimit(String clientId, RateLimit rateLimit) {
    Instant now = Instant.now();
    if (Duration.between(rateLimit.lastResetTime(), now).compareTo(quotaResetPeriod) >= 0) {
      rateLimit = rateLimit.resetQuotaUsed();
      LOGGER.info("Quota reset for client: {}", clientId);
    }

    if (rateLimit.quotaUsed() >= quotaLimit) {
      Duration retryAfter = Duration.between(now, rateLimit.lastResetTime().plus(quotaResetPeriod));
      LOGGER.warn("Quota limit exceeded for client: {}. Retry after: {}", clientId, retryAfter);
      circuitBreaker.recordFailure();
      return new RateLimitResult(false, retryAfter);
    }

    RateLimit updatedRateLimit = rateLimit.incrementQuotaUsed();
    LOGGER.debug("Request allowed for client: {}. Quota used: {}", clientId, updatedRateLimit.quotaUsed());
    circuitBreaker.recordSuccess();
    return new RateLimitResult(true, Duration.ZERO);
  }
}