package com.iluwatar.ratelimiter.infrastructure.configs.throttling;

import com.iluwatar.ratelimiter.domain.ClientType;
import com.iluwatar.ratelimiter.domain.ThrottlingStrategyType;
import com.iluwatar.ratelimiter.infrastructure.configs.ratelimit.RateLimitConfig;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ThrottlingStrategyConfig {

  private final RateLimitConfig config;
  private final ThrottlingStrategyFactory throttlingStrategyFactory;
  private final ConcurrentHashMap<String, Map<ClientType, ThrottlingStrategy>> strategyCache = new ConcurrentHashMap<>();
  private ThrottlingStrategy defaultThrottlingStrategy;

  @EventListener(RefreshScopeRefreshedEvent.class)
  public void onRefresh() {
    strategyCache.clear();
    defaultThrottlingStrategy = throttlingStrategyFactory.createStrategy(config.getDefaultThrottlingStrategy());
  }

  public ThrottlingStrategy getThrottlingStrategy(String endpoint, ClientType clientType) {
    return strategyCache
        .computeIfAbsent(endpoint, k -> new ConcurrentHashMap<>())
        .computeIfAbsent(clientType, k -> {
          ThrottlingStrategyType type = config.getThrottlingStrategiesByEndpointAndClientType()
              .getOrDefault(endpoint, Map.of())
              .getOrDefault(clientType, config.getDefaultThrottlingStrategy());
          return throttlingStrategyFactory.createStrategy(type);
        });
  }

  public ThrottlingStrategy getDefaultThrottlingStrategy() {
    if (defaultThrottlingStrategy == null) {
      defaultThrottlingStrategy = throttlingStrategyFactory.createStrategy(config.getDefaultThrottlingStrategy());
    }
    return defaultThrottlingStrategy;
  }
}