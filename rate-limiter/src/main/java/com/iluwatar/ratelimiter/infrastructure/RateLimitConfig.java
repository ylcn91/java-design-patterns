package com.iluwatar.ratelimiter.infrastructure;

import com.iluwatar.ratelimiter.domain.AlgorithmType;
import com.iluwatar.ratelimiter.domain.ClientType;
import com.iluwatar.ratelimiter.domain.ThrottlingStrategyType;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "ratelimiter")
public class RateLimitConfig {

  @Min(1)
  private int standardRequestsPerSecond;

  @Min(1)
  private int premiumRequestsPerSecond;

  @Min(1)
  private int vipRequestsPerSecond;

  @Min(1)
  private int enterpriseRequestsPerSecond;

  @Min(1)
  private int customRequestsPerSecond;

  @Min(1)
  private int internalRequestsPerSecond;

  @NotNull
  private AlgorithmType defaultAlgorithm;

  private Map<String, AlgorithmType> algorithmByEndpoint;

  @Min(1)
  private long slidingWindowSize;

  @Min(1)
  private long bucketCapacity;

  @Min(1)
  private long refillRate;

  @Min(1)
  private long timeWindowInMillis = 10;

  @Min(1)
  private int circuitBreakerFailureThreshold;

  @Min(1)
  private long circuitBreakerResetTimeoutMillis;

  @Min(1)
  private long exponentialBackoffInitialDelay;

  @Min(1)
  private double exponentialBackoffMultiplier;

  private Map<String, Map<ClientType, ThrottlingStrategyType>> throttlingStrategiesByEndpointAndClientType;

  @NotNull
  private ThrottlingStrategyType defaultThrottlingStrategy = ThrottlingStrategyType.DELAY;

  private Map<String, Integer> endpointRequestsPerSecond;
  private Map<String, Map<String, Integer>> timeBasedRequestsPerSecond;

  @Min(1)
  private long quotaLimit;
}