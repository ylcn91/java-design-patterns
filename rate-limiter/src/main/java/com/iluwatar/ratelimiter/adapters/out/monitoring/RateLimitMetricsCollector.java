package com.iluwatar.ratelimiter.adapters.out.monitoring;

import com.iluwatar.ratelimiter.application.port.out.MetricsCollector;
import com.iluwatar.ratelimiter.domain.Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RateLimitMetricsCollector implements MetricsCollector {

  @Override
  public void recordRateLimitExceeded(String endpoint, Client client) {
    LOGGER.warn("Rate limit exceeded for client: {} on endpoint: {}", client.id(), endpoint);
  }

  @Override
  public void recordRateLimitSuccess(String endpoint, Client client) {
    LOGGER.info("Rate limit check successful for client: {} on endpoint: {}", client.id(), endpoint);
  }
}
