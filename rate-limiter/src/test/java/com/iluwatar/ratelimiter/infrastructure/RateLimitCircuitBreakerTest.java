package com.iluwatar.ratelimiter.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

class RateLimitCircuitBreakerTest {

  private RateLimitCircuitBreaker circuitBreaker;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    circuitBreaker = new RateLimitCircuitBreaker(5, Duration.ofSeconds(60));
  }

  @Test
  void givenClosedCircuit_whenSuccessRecorded_thenStaysClosed() {
    // given
    // Circuit is initially closed

    // when
    circuitBreaker.recordSuccess();

    // then
    assertTrue(circuitBreaker.isCircuitClosed());
  }

  @Test
  void givenClosedCircuit_whenFailureRecorded_thenStaysClosedUntilThreshold() {
    // given
    // Circuit is initially closed

    // when
    for (int i = 0; i < 4; i++) {
      circuitBreaker.recordFailure();
    }

    // then
    assertTrue(circuitBreaker.isCircuitClosed());
  }

  @Test
  void givenClosedCircuit_whenFailuresExceedThreshold_thenOpensCircuit() {
    // given
    // Circuit is initially closed

    // when
    for (int i = 0; i < 5; i++) {
      circuitBreaker.recordFailure();
    }

    // then
    assertFalse(circuitBreaker.isCircuitClosed());
  }

  @Test
  void givenOpenCircuit_whenResetTimeoutExpires_thenHalfOpens() {
    // given
    // Circuit is initially closed
    for (int i = 0; i < 5; i++) {
      circuitBreaker.recordFailure();
    }
    assertFalse(circuitBreaker.isCircuitClosed());

    // when
    circuitBreaker.resetTimeout = Duration.ofSeconds(1);
    circuitBreaker.lastOpenTime = Instant.now().minusSeconds(2);

    // then
    assertTrue(circuitBreaker.isCircuitClosed());
  }

  @Test
  void givenHalfOpenCircuit_whenSuccessRecorded_thenClosesCircuit() {
    // given
    for (int i = 0; i < 5; i++) {
      circuitBreaker.recordFailure();
    }
    circuitBreaker.resetTimeout = Duration.ofSeconds(1);
    circuitBreaker.lastOpenTime = Instant.now().minusSeconds(2);

    // Circuit should now be open
    assertFalse(circuitBreaker.isCircuitClosed());
    assertTrue(circuitBreaker.isOpen.get());
    assertFalse(circuitBreaker.isHalfOpen.get());

    // Simulate timeout expiry to transition to half-open state
    circuitBreaker.lastOpenTime = Instant.now().minusSeconds(2);

    // Verify transition to half-open state
    assertTrue(circuitBreaker.isCircuitClosed());
    assertTrue(circuitBreaker.isHalfOpen.get());
    assertFalse(circuitBreaker.isOpen.get());

    // when
    circuitBreaker.recordSuccess();

    // then
    assertTrue(circuitBreaker.isCircuitClosed());
    assertFalse(circuitBreaker.isOpen.get());
    assertFalse(circuitBreaker.isHalfOpen.get());
  }





  @Test
  void givenHalfOpenCircuit_whenFailureRecorded_thenOpensCircuit() {
    // given
    for (int i = 0; i < 5; i++) {
      circuitBreaker.recordFailure();
    }
    circuitBreaker.resetTimeout = Duration.ofSeconds(1);
    circuitBreaker.lastOpenTime = Instant.now().minusSeconds(2);

    // Circuit should now be open
    assertFalse(circuitBreaker.isCircuitClosed());
    assertTrue(circuitBreaker.isOpen.get());
    assertFalse(circuitBreaker.isHalfOpen.get());

    // Simulate timeout expiry to transition to half-open state
    circuitBreaker.lastOpenTime = Instant.now().minusSeconds(2);

    // Verify transition to half-open state
    assertTrue(circuitBreaker.isCircuitClosed());
    assertTrue(circuitBreaker.isHalfOpen.get());
    assertFalse(circuitBreaker.isOpen.get());

    // when
    circuitBreaker.recordFailure();

    // then
    assertFalse(circuitBreaker.isCircuitClosed());
    assertTrue(circuitBreaker.isOpen.get());
    assertFalse(circuitBreaker.isHalfOpen.get());
  }

}

