package com.iluwatar.ratelimiter.infrastructure.configs.throttling;

import com.iluwatar.ratelimiter.infrastructure.configs.ratelimit.RateLimitConfig;
import com.iluwatar.ratelimiter.domain.ThrottlingStrategyType;
import com.iluwatar.ratelimiter.infrastructure.throttling.DelayThrottlingStrategy;
import com.iluwatar.ratelimiter.infrastructure.throttling.ExponentialBackoffThrottlingStrategy;
import com.iluwatar.ratelimiter.infrastructure.throttling.RejectThrottlingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThrottlingStrategyFactory {

  private final RateLimitConfig config;

  public ThrottlingStrategy createStrategy(ThrottlingStrategyType type) {
    LOGGER.info("Creating throttling strategy: {}", type);
    return switch (type) {
      case DELAY -> new DelayThrottlingStrategy();
      case REJECT -> new RejectThrottlingStrategy();
      case EXPONENTIAL_BACKOFF -> new ExponentialBackoffThrottlingStrategy(
          config.getExponentialBackoffInitialDelay(), config.getExponentialBackoffMultiplier()
      );
      default -> throw new IllegalArgumentException("Unsupported throttling strategy type: " + type);
    };
  }
}
