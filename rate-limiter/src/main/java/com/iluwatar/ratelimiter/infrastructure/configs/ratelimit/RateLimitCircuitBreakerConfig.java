package com.iluwatar.ratelimiter.infrastructure.configs.ratelimit;

import com.iluwatar.ratelimiter.infrastructure.RateLimitCircuitBreaker;
import java.time.Duration;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

@Configuration
public class RateLimitCircuitBreakerConfig {

  @Bean
  @RefreshScope
  @Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
  public RateLimitCircuitBreaker rateLimitCircuitBreaker(RateLimitConfig config) {
    return new RateLimitCircuitBreaker(
        config.getCircuitBreakerFailureThreshold(),
        Duration.ofMillis(config.getCircuitBreakerResetTimeoutMillis())
    );
  }
}