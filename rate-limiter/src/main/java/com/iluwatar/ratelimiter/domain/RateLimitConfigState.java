package com.iluwatar.ratelimiter.domain;

import java.util.Map;

public record RateLimitConfigState(
    int standardRequestsPerSecond,
    int premiumRequestsPerSecond,
    AlgorithmType defaultAlgorithm,
    Map<String, AlgorithmType> algorithmByEndpoint,
    long slidingWindowSize,
    long bucketCapacity,
    long refillRate,
    long timeWindowInMillis,
    int circuitBreakerFailureThreshold,
    long circuitBreakerResetTimeoutMillis,
    long exponentialBackoffInitialDelay,
    double exponentialBackoffMultiplier,
    Map<String, Map<ClientType, ThrottlingStrategyType>> throttlingStrategiesByEndpointAndClientType,
    ThrottlingStrategyType defaultThrottlingStrategy
) { }