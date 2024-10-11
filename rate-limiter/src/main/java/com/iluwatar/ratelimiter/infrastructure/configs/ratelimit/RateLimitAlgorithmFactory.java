package com.iluwatar.ratelimiter.infrastructure.configs.ratelimit;

import com.iluwatar.ratelimiter.domain.AlgorithmType;
import com.iluwatar.ratelimiter.infrastructure.RateLimitCircuitBreaker;
import com.iluwatar.ratelimiter.infrastructure.algorithms.FixedWindowRateLimiter;
import com.iluwatar.ratelimiter.infrastructure.algorithms.LeakyBucketRateLimiter;
import com.iluwatar.ratelimiter.infrastructure.algorithms.QuotaRateLimiter;
import com.iluwatar.ratelimiter.infrastructure.algorithms.RateLimitAlgorithm;
import com.iluwatar.ratelimiter.infrastructure.algorithms.SlidingWindowRateLimiter;
import com.iluwatar.ratelimiter.infrastructure.algorithms.TokenBucketRateLimiter;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class RateLimitAlgorithmFactory {

  private final RateLimitConfig config;
  private final RateLimitCircuitBreaker circuitBreaker;
  private final Map<AlgorithmType, Supplier<RateLimitAlgorithm>> algorithmCreators = new EnumMap<>(AlgorithmType.class);

  @EventListener(RefreshScopeRefreshedEvent.class)
  public void onRefresh() {
    algorithmCreators.clear();
    initAlgorithmCreators();
  }

  @PostConstruct
  private void initAlgorithmCreators() {
    algorithmCreators.put(
        AlgorithmType.FIXED_WINDOW, () -> new FixedWindowRateLimiter(config, circuitBreaker));
    algorithmCreators.put(AlgorithmType.SLIDING_WINDOW, () -> new SlidingWindowRateLimiter(config, circuitBreaker));
    algorithmCreators.put(AlgorithmType.TOKEN_BUCKET, () -> new TokenBucketRateLimiter(config, circuitBreaker));
    algorithmCreators.put(AlgorithmType.LEAKY_BUCKET, () -> new LeakyBucketRateLimiter(config, circuitBreaker));
    algorithmCreators.put(AlgorithmType.QUOTA, () -> new QuotaRateLimiter(config, circuitBreaker));
  }

  public RateLimitAlgorithm createAlgorithm(AlgorithmType type) {
    Supplier<RateLimitAlgorithm> creator = algorithmCreators.get(type);
    if (creator == null) {
      throw new IllegalArgumentException("Unsupported algorithm type: " + type);
    }
    return creator.get();
  }
}
