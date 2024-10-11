package com.iluwatar.ratelimiter.application.service;

import com.iluwatar.ratelimiter.application.port.in.RateLimitUseCase;
import com.iluwatar.ratelimiter.application.port.out.MetricsCollector;
import com.iluwatar.ratelimiter.application.port.out.RateLimitRepository;
import com.iluwatar.ratelimiter.domain.Client;
import com.iluwatar.ratelimiter.domain.Policy;
import com.iluwatar.ratelimiter.domain.RateLimit;
import com.iluwatar.ratelimiter.domain.RateLimitResult;
import com.iluwatar.ratelimiter.infrastructure.configs.PolicyConfig;
import com.iluwatar.ratelimiter.infrastructure.configs.ratelimit.RateLimitContext;
import com.iluwatar.ratelimiter.infrastructure.configs.throttling.ThrottlingStrategy;
import com.iluwatar.ratelimiter.infrastructure.configs.throttling.ThrottlingStrategyContext;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService implements RateLimitUseCase {

  private final RateLimitContext rateLimitContext;
  private final RateLimitRepository rateLimitRepository;
  private final MetricsCollector metricsCollector;
  private final ClientIdentificationService clientIdentificationService;
  private final PolicyConfig policyConfig;
  private final TimeRangeService timeRangeService;
  private final ThrottlingStrategyContext throttlingStrategyContext;

  @Override
  public RateLimitResult checkRateLimit(String endpoint, String clientId) {
    LOGGER.info("Checking rate limit for client: {} on endpoint: {}", clientId, endpoint);
    Client client = clientIdentificationService.identifyClient(clientId);
    LOGGER.info("Identified client: {}", client);

    String timeRange = timeRangeService.determineTimeRange(LocalTime.now());
    LOGGER.info("Determined time range: {}", timeRange);

    Policy policy = policyConfig.getPolicy(endpoint, client.type(), timeRange);
    LOGGER.info("Policy for client: {}", policy);

    RateLimit rateLimit = rateLimitRepository.getRateLimit(clientId, endpoint);
    LOGGER.info("Fetched rate limit: {}", rateLimit);

    RateLimit updatedRateLimit = new RateLimit(client, policy, rateLimit.lastResetTime(), rateLimit.requestCount(), rateLimit.quotaUsed());
    LOGGER.info("Updated rate limit before check: {}", updatedRateLimit);

    RateLimitResult result = rateLimitContext.checkRateLimit(endpoint, updatedRateLimit);
    LOGGER.info("Rate limit result: {}", result);

    ThrottlingStrategy throttlingStrategy = throttlingStrategyContext.getThrottlingStrategy(endpoint, client.type());
    LOGGER.info("Using throttling strategy: {}", throttlingStrategy.getClass().getSimpleName());
    try {
      throttlingStrategy.throttle(result);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Throttling was interrupted", e);
    }

    if (result.allowed()) {
      LOGGER.info("Rate limit allowed for client: {}. Incrementing request count and quota used.", clientId);
      updatedRateLimit = updatedRateLimit.incrementRequestCount().incrementQuotaUsed();
      LOGGER.info("Incremented request count and quota used: {}", updatedRateLimit);
      rateLimitRepository.saveRateLimit(clientId, endpoint, updatedRateLimit);
      metricsCollector.recordRateLimitSuccess(endpoint, client);
    } else {
      LOGGER.info("Rate limit exceeded for client: {}. Not incrementing counters.", clientId);
      metricsCollector.recordRateLimitExceeded(endpoint, client);
    }

    return result;
  }
}