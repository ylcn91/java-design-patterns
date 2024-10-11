package com.iluwatar.ratelimiter.infrastructure.configs.throttling;

import com.iluwatar.ratelimiter.domain.ClientType;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ThrottlingStrategyContext {

  private final ThrottlingStrategyConfig throttlingStrategyConfig;

  @EventListener(RefreshScopeRefreshedEvent.class)
  public void onRefresh() {
    throttlingStrategyConfig.onRefresh();
  }

  public ThrottlingStrategy getThrottlingStrategy(String endpoint, ClientType clientType) {
    return throttlingStrategyConfig.getThrottlingStrategy(endpoint, clientType);
  }

  public ThrottlingStrategy getDefaultThrottlingStrategy() {
    return throttlingStrategyConfig.getDefaultThrottlingStrategy();
  }
}
