package com.iluwatar.ratelimiter.application.service;

import com.iluwatar.ratelimiter.domain.RateLimitConfigState;
import com.iluwatar.ratelimiter.infrastructure.RateLimitCircuitBreaker;
import com.iluwatar.ratelimiter.infrastructure.configs.ratelimit.RateLimitAlgorithmFactory;
import com.iluwatar.ratelimiter.infrastructure.configs.ratelimit.RateLimitConfig;
import com.iluwatar.ratelimiter.infrastructure.configs.ratelimit.RateLimitContext;
import com.iluwatar.ratelimiter.infrastructure.configs.throttling.ThrottlingStrategyConfig;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RefreshScope
@RequiredArgsConstructor
public class DynamicConfigService {

  private final ReadWriteLock configLock = new ReentrantReadWriteLock();

  private final RateLimitConfig rateLimitConfig;
  private final ApplicationEventPublisher eventPublisher;
  private final ApplicationContext applicationContext;

  public record ConfigUpdateResult(RateLimitConfigState oldConfig, RateLimitConfigState newConfig) {}

  public ConfigUpdateResult updateConfig(RateLimitConfig newConfig) {
    LOGGER.info("Updating rate limit configuration");
    configLock.writeLock().lock();
    try {
      RateLimitConfigState oldConfigState = getCurrentConfigState();
      LOGGER.info("old configs: {}", oldConfigState);

      updateConfigurationFields(newConfig);

      RateLimitCircuitBreaker circuitBreaker = applicationContext.getBean(RateLimitCircuitBreaker.class);
      LOGGER.info("resetting circuitBreaker");
      circuitBreaker.reset();

      RateLimitConfigState newConfigState = getCurrentConfigState();
      LOGGER.info("new configs: {} ", newConfigState);
      // Publish a refresh event to update all @RefreshScope beans
      eventPublisher.publishEvent(new RefreshScopeRefreshedEvent());

      // Manually refresh config beans that need it
      refreshConfigBeans();

      return new ConfigUpdateResult(oldConfigState, newConfigState);
    } finally {
      configLock.writeLock().unlock();
    }
  }

  private void refreshConfigBeans() {
    applicationContext.getBean(RateLimitContext.class).onRefresh();
    applicationContext.getBean(ThrottlingStrategyConfig.class).onRefresh();
    applicationContext.getBean(RateLimitAlgorithmFactory.class).onRefresh();
  }


  private void updateConfigurationFields(RateLimitConfig newConfig) {
    rateLimitConfig.setStandardRequestsPerSecond(newConfig.getStandardRequestsPerSecond());
    rateLimitConfig.setPremiumRequestsPerSecond(newConfig.getPremiumRequestsPerSecond());
    rateLimitConfig.setDefaultAlgorithm(newConfig.getDefaultAlgorithm());
    rateLimitConfig.setAlgorithmByEndpoint(newConfig.getAlgorithmByEndpoint());
    rateLimitConfig.setSlidingWindowSize(newConfig.getSlidingWindowSize());
    rateLimitConfig.setBucketCapacity(newConfig.getBucketCapacity());
    rateLimitConfig.setRefillRate(newConfig.getRefillRate());
    rateLimitConfig.setTimeWindowInMillis(newConfig.getTimeWindowInMillis());
    rateLimitConfig.setCircuitBreakerFailureThreshold(newConfig.getCircuitBreakerFailureThreshold());
    rateLimitConfig.setCircuitBreakerResetTimeoutMillis(newConfig.getCircuitBreakerResetTimeoutMillis());
    rateLimitConfig.setExponentialBackoffInitialDelay(newConfig.getExponentialBackoffInitialDelay());
    rateLimitConfig.setExponentialBackoffMultiplier(newConfig.getExponentialBackoffMultiplier());
    rateLimitConfig.setThrottlingStrategiesByEndpointAndClientType(newConfig.getThrottlingStrategiesByEndpointAndClientType());
    rateLimitConfig.setDefaultThrottlingStrategy(newConfig.getDefaultThrottlingStrategy());
  }


  private RateLimitConfigState getCurrentConfigState() {
    configLock.readLock().lock();
    try {
      return new RateLimitConfigState(
          rateLimitConfig.getStandardRequestsPerSecond(),
          rateLimitConfig.getPremiumRequestsPerSecond(),
          rateLimitConfig.getDefaultAlgorithm(),
          rateLimitConfig.getAlgorithmByEndpoint(),
          rateLimitConfig.getSlidingWindowSize(),
          rateLimitConfig.getBucketCapacity(),
          rateLimitConfig.getRefillRate(),
          rateLimitConfig.getTimeWindowInMillis(),
          rateLimitConfig.getCircuitBreakerFailureThreshold(),
          rateLimitConfig.getCircuitBreakerResetTimeoutMillis(),
          rateLimitConfig.getExponentialBackoffInitialDelay(),
          rateLimitConfig.getExponentialBackoffMultiplier(),
          rateLimitConfig.getThrottlingStrategiesByEndpointAndClientType(),
          rateLimitConfig.getDefaultThrottlingStrategy()
      );
    } finally {
      configLock.readLock().unlock();
    }
  }
}

