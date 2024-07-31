package com.iluwatar.ratelimiter.infrastructure.algorithms;

import com.iluwatar.ratelimiter.domain.Client;
import com.iluwatar.ratelimiter.domain.ClientType;
import com.iluwatar.ratelimiter.domain.Policy;
import com.iluwatar.ratelimiter.domain.RateLimit;
import com.iluwatar.ratelimiter.domain.RateLimitResult;
import com.iluwatar.ratelimiter.infrastructure.RateLimitCircuitBreaker;
import com.iluwatar.ratelimiter.infrastructure.RateLimitConfig;
import com.iluwatar.ratelimiter.infrastructure.AbstractRateLimitAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class LeakyBucketRateLimiterTest {

  @Mock
  private RateLimitConfig config;

  @Mock
  private RateLimitCircuitBreaker circuitBreaker;

  private LeakyBucketRateLimiter leakyBucketRateLimiter;

  @BeforeEach
  void setUp() {
    when(config.getTimeWindowInMillis()).thenReturn(1000L);
    when(config.getSlidingWindowSize()).thenReturn(1000L);
    when(config.getBucketCapacity()).thenReturn(10L);
    when(config.getRefillRate()).thenReturn(1L);
    leakyBucketRateLimiter = new LeakyBucketRateLimiter(config, circuitBreaker);
  }

  @Nested
  @DisplayName("Basic functionality tests")
  class BasicFunctionalityTests {

    @Test
    @DisplayName("Should allow requests within rate limit")
    void shouldAllowRequestsWithinRateLimit() {
      // Given
      RateLimit rateLimit = new RateLimit(
          new Client("testClient", ClientType.STANDARD),
          new Policy(5),
          Instant.now(),
          0,
          0
      );

      // When
      RateLimitResult result = leakyBucketRateLimiter.doRateLimitCheck(rateLimit);

      // Then
      assertTrue(result.allowed());
      assertEquals(Duration.ZERO, result.retryAfter());
      verify(circuitBreaker).recordSuccess();
    }

    @Test
    @DisplayName("Should reject requests exceeding rate limit")
    void shouldRejectRequestsExceedingRateLimit() {
      RateLimit rateLimit = new RateLimit(
          new Client("testClient", ClientType.STANDARD),
          new Policy(5),
          Instant.now(),
          0,
          0
      );

      IntStream.range(0, 10).forEach(i -> leakyBucketRateLimiter.doRateLimitCheck(rateLimit));
      RateLimitResult result = leakyBucketRateLimiter.doRateLimitCheck(rateLimit);

      assertFalse(result.allowed());
      assertTrue(result.retryAfter().compareTo(Duration.ZERO) > 0);
      verify(circuitBreaker).recordFailure();
    }
  }

  @Nested
  @DisplayName("Edge case tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should allow requests after bucket leaks")
    void shouldAllowRequestsAfterBucketLeaks() throws InterruptedException {
      // Given
      RateLimit rateLimit = new RateLimit(
          new Client("testClient", ClientType.STANDARD),
          new Policy(5),
          Instant.now(),
          0,
          0
      );

      // When
      IntStream.range(0, 5).forEach(i -> leakyBucketRateLimiter.doRateLimitCheck(rateLimit));
      Thread.sleep(1100); // Wait for slightly more than the time window
      RateLimitResult result = leakyBucketRateLimiter.doRateLimitCheck(rateLimit);

      // Then
      assertTrue(result.allowed());
      assertEquals(Duration.ZERO, result.retryAfter());
    }

    @Test
    @DisplayName("Should handle requests exactly at rate limit")
    void shouldHandleRequestsExactlyAtRateLimit() {
      RateLimit rateLimit = new RateLimit(
          new Client("testClient", ClientType.STANDARD),
          new Policy(5),
          Instant.now(),
          0,
          0
      );

      IntStream.range(0, 10).forEach(i -> leakyBucketRateLimiter.doRateLimitCheck(rateLimit));
      RateLimitResult result = leakyBucketRateLimiter.doRateLimitCheck(rateLimit);

      assertFalse(result.allowed());
      assertTrue(result.retryAfter().compareTo(Duration.ZERO) > 0);
    }



    @Test
    @DisplayName("Should handle null client ID")
    void shouldHandleNullClientId() {
      // Given
      RateLimit rateLimit = new RateLimit(
          new Client(null, ClientType.STANDARD),
          new Policy(5),
          Instant.now(),
          0,
          0
      );

      // When/Then
      assertThrows(NullPointerException.class, () -> leakyBucketRateLimiter.doRateLimitCheck(rateLimit));
    }
  }

  @Nested
  @DisplayName("Performance tests")
  class PerformanceTests {

    @Test
    @DisplayName("Should handle high concurrency")
    void shouldHandleHighConcurrency() throws InterruptedException {
      // Given
      int concurrentRequests = 1000;
      ExecutorService executorService = Executors.newFixedThreadPool(10);

      // When
      IntStream.range(0, concurrentRequests).forEach(i ->
          executorService.submit(() -> {
            RateLimit rateLimit = new RateLimit(
                new Client("testClient" + (i % 10), ClientType.STANDARD),
                new Policy(100),
                Instant.now(),
                0,
                0
            );
            leakyBucketRateLimiter.doRateLimitCheck(rateLimit);
          })
      );

      executorService.shutdown();
      boolean terminated = executorService.awaitTermination(10, TimeUnit.SECONDS);

      // Then
      assertTrue(terminated, "Execution did not complete in time");
    }
  }

  @Nested
  @DisplayName("Worst case scenarios")
  class WorstCaseScenarios {

    @Test
    @DisplayName("Should handle burst of requests")
    void shouldHandleBurstOfRequests() {
      // Given
      RateLimit rateLimit = new RateLimit(
          new Client("testClient", ClientType.STANDARD),
          new Policy(5),
          Instant.now(),
          0,
          0
      );

      // When
      IntStream.range(0, 10).forEach(i -> leakyBucketRateLimiter.doRateLimitCheck(rateLimit));
      RateLimitResult result = leakyBucketRateLimiter.doRateLimitCheck(rateLimit);

      // Then
      assertFalse(result.allowed());
      assertTrue(result.retryAfter().compareTo(Duration.ZERO) > 0);
    }

    @Test
    @DisplayName("Should handle rapid requests over time")
    void shouldHandleRapidRequestsOverTime() throws InterruptedException {
      // Given
      RateLimit rateLimit = new RateLimit(
          new Client("testClient", ClientType.STANDARD),
          new Policy(5),
          Instant.now(),
          0,
          0
      );

      // When
      for (int i = 0; i < 3; i++) {
        IntStream.range(0, 5).forEach(j -> leakyBucketRateLimiter.doRateLimitCheck(rateLimit));
        Thread.sleep(1100); // Wait for slightly more than the time window
      }
      RateLimitResult result = leakyBucketRateLimiter.doRateLimitCheck(rateLimit);

      // Then
      assertTrue(result.allowed());
      assertEquals(Duration.ZERO, result.retryAfter());
    }
  }

  @Nested
  @DisplayName("Additional edge cases")
  class AdditionalEdgeCases {

    @Test
    @DisplayName("Should handle very high rate limit")
    void shouldHandleVeryHighRateLimit() {
      // Given
      RateLimit rateLimit = new RateLimit(
          new Client("testClient", ClientType.STANDARD),
          new Policy(Integer.MAX_VALUE),
          Instant.now(),
          0,
          0
      );

      // When
      RateLimitResult result = leakyBucketRateLimiter.doRateLimitCheck(rateLimit);

      // Then
      assertTrue(result.allowed());
      assertEquals(Duration.ZERO, result.retryAfter());
    }

    @Test
    @DisplayName("Should handle requests after long inactivity")
    void shouldHandleRequestsAfterLongInactivity() throws InterruptedException {
      // Given
      RateLimit rateLimit = new RateLimit(
          new Client("testClient", ClientType.STANDARD),
          new Policy(5),
          Instant.now(),
          0,
          0
      );

      // When
      IntStream.range(0, 5).forEach(i -> leakyBucketRateLimiter.doRateLimitCheck(rateLimit));
      Thread.sleep(5000); // Wait for 5 seconds
      RateLimitResult result = leakyBucketRateLimiter.doRateLimitCheck(rateLimit);

      // Then
      assertTrue(result.allowed());
      assertEquals(Duration.ZERO, result.retryAfter());
    }
  }


  @Nested
  @DisplayName("Optimistic check tests")
  class OptimisticCheckTests {

    @Test
    @DisplayName("Optimistic check should return same result as full check")
    void optimisticCheckShouldReturnSameResultAsFullCheck() {
      // Given
      RateLimit rateLimit = new RateLimit(
          new Client("testClient", ClientType.STANDARD),
          new Policy(5),
          Instant.now(),
          0,
          0
      );

      // When
      RateLimitResult optimisticResult = leakyBucketRateLimiter.doOptimisticRateLimitCheck(rateLimit, new AbstractRateLimitAlgorithm.ClientData());
      RateLimitResult fullResult = leakyBucketRateLimiter.doRateLimitCheck(rateLimit);

      // Then
      assertEquals(fullResult.allowed(), optimisticResult.allowed());
      assertEquals(fullResult.retryAfter(), optimisticResult.retryAfter());
    }
  }

  @Nested
  @DisplayName("Time-based tests")
  class TimeBasedTests {

    @Test
    @DisplayName("Should allow requests at exact intervals")
    void shouldAllowRequestsAtExactIntervals() throws InterruptedException {
      // Given
      RateLimit rateLimit = new RateLimit(
          new Client("testClient", ClientType.STANDARD),
          new Policy(1),
          Instant.now(),
          0,
          0
      );

      // When
      for (int i = 0; i < 5; i++) {
        RateLimitResult result = leakyBucketRateLimiter.doRateLimitCheck(rateLimit);
        assertTrue(result.allowed());
        Thread.sleep(1000); // Wait for exactly one second
      }

      // Then
      RateLimitResult finalResult = leakyBucketRateLimiter.doRateLimitCheck(rateLimit);
      assertTrue(finalResult.allowed());
    }
  }

}