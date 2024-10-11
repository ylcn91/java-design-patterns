package com.iluwatar.ratelimiter.infrastructure.configs.ratelimit;

import com.iluwatar.ratelimiter.application.service.RateLimitExemptionService;
import com.iluwatar.ratelimiter.domain.AlgorithmType;
import com.iluwatar.ratelimiter.domain.RateLimit;
import com.iluwatar.ratelimiter.domain.RateLimitResult;
import com.iluwatar.ratelimiter.infrastructure.algorithms.RateLimitAlgorithm;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitContext {

  private final RateLimitConfig config;
  private final RateLimitAlgorithmFactory algorithmFactory;
  private final RateLimitExemptionService exemptionService;
  private Map<String, AlgorithmType> algorithmByEndpoint;
  private final ConcurrentHashMap<AlgorithmType, RateLimitAlgorithm> algorithmCache = new ConcurrentHashMap<>();

  @EventListener(RefreshScopeRefreshedEvent.class)
  public void onRefresh() {
    LOGGER.info("Configuration refresh detected. Clearing algorithm cache and reloading configuration.");
    algorithmCache.clear();
    loadConfig();
  }

  @PostConstruct
  private void loadConfig() {
    LOGGER.info("Loading rate limit configuration...");
    algorithmByEndpoint = config.getAlgorithmByEndpoint();
    if (algorithmByEndpoint == null || algorithmByEndpoint.isEmpty()) {
      throw new IllegalStateException("RateLimit configuration is invalid: algorithmByEndpoint map is null or empty.");
    }
    LOGGER.info("Rate limit configuration loaded successfully.");
  }

  public RateLimitResult checkRateLimit(String endpoint, RateLimit rateLimit) {
    LOGGER.info("Checking rate limit for endpoint: {}, client: {}", endpoint, rateLimit.client().id());

    if (exemptionService.isExempt(rateLimit.client())) {
      LOGGER.info("Client: {} is exempt from rate limiting.", rateLimit.client().id());
      return new RateLimitResult(true, Duration.ZERO);
    }

    AlgorithmType algorithmType = algorithmByEndpoint.getOrDefault(endpoint, config.getDefaultAlgorithm());
    LOGGER.info("Using algorithm type: {} for endpoint: {}", algorithmType, endpoint);

    RateLimitAlgorithm algorithm = algorithmCache.computeIfAbsent(algorithmType, type -> {
      LOGGER.info("Creating rate limit algorithm instance for type: {}", type);
      return algorithmFactory.createAlgorithm(type);
    });

    try {
      RateLimitResult result = algorithm.checkRateLimit(rateLimit);
      LOGGER.info("Rate limit check result for endpoint: {}, client: {}: {}", endpoint, rateLimit.client().id(), result);
      return result;
    } catch (Exception e) {
      LOGGER.error("Error occurred during rate limit check for endpoint: {}, client: {}", endpoint, rateLimit.client().id(), e);
      throw e;
    }
  }
}
