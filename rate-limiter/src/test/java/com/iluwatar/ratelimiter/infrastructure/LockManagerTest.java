package com.iluwatar.ratelimiter.infrastructure;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.locks.StampedLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

class LockManagerTest {

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void givenOptimisticReadLock_whenActionExecuted_thenSuccess() {
    // given
    StampedLock lock = new StampedLock();

    // when
    String result = LockManager.withOptimisticRead(lock, () -> "optimistic", () -> "write");

    // then
    assertEquals("optimistic", result);
  }

  @Test
  void givenOptimisticReadLockFails_whenActionExecuted_thenFallbackToWriteLock() {
    // given
    StampedLock lock = spy(new StampedLock());
    doReturn(false).when(lock).validate(anyLong());

    // when
    String result = LockManager.withOptimisticRead(lock, () -> "optimistic", () -> "write");

    // then
    assertEquals("write", result);
  }

  @Test
  void givenWriteLock_whenActionExecuted_thenSuccess() {
    // given
    StampedLock lock = new StampedLock();

    // when
    String result = LockManager.withWriteLock(lock, () -> "write");

    // then
    assertEquals("write", result);
  }

  @Test
  void givenReadLock_whenActionExecuted_thenSuccess() {
    // given
    StampedLock lock = new StampedLock();

    // when
    String result = LockManager.withReadLock(lock, () -> "read");

    // then
    assertEquals("read", result);
  }
}
