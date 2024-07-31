package com.iluwatar.ratelimiter.infrastructure;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LockManager {

  private static final int RETRY_LIMIT = 3;

  public static <T> T withWriteLock(StampedLock lock, Supplier<T> action) {
    long stamp = 0;
    int retryCount = 0;
    try {
      while (retryCount < RETRY_LIMIT) {
        try {
          LOGGER.info("Attempting to acquire write lock... Attempt {}", retryCount + 1);
          stamp = tryAcquireWriteLock(lock, 5000);  // 5 seconds timeout
          if (stamp != 0) {
            LOGGER.info("Write lock acquired: {}", stamp);
            T result = action.get();
            LOGGER.info("Write action executed successfully");
            return result;
          }
        } catch (InterruptedException e) {
          LOGGER.error("Interrupted while trying to acquire write lock", e);
          Thread.currentThread().interrupt();
          throw new RuntimeException("Interrupted while trying to acquire write lock", e);
        }
        retryCount++;
        LOGGER.info("Retrying to acquire write lock... Attempt {}", retryCount + 1);
      }
      throw new RuntimeException("Failed to acquire write lock after retries");
    } catch (Exception e) {
      LOGGER.error("Exception during write action", e);
      throw e;
    } finally {
      if (stamp != 0) {
        lock.unlockWrite(stamp);
        LOGGER.info("Write lock released: {}", stamp);
      }
    }
  }

  public static <T> T withReadLock(StampedLock lock, Supplier<T> action) {
    long stamp = 0;
    try {
      LOGGER.info("Attempting to acquire read lock...");
      stamp = lock.readLock();
      LOGGER.info("Read lock acquired: {}", stamp);
      T result = action.get();
      LOGGER.info("Read action executed successfully");
      return result;
    } catch (Exception e) {
      LOGGER.error("Exception during read action", e);
      throw e;
    } finally {
      if (stamp != 0) {
        lock.unlockRead(stamp);
        LOGGER.info("Read lock released: {}", stamp);
      }
    }
  }

  public static <T> T withOptimisticRead(StampedLock lock, Supplier<T> optimisticReadAction, Supplier<T> writeAction) {
    long stamp = lock.tryOptimisticRead();
    LOGGER.info("Optimistic read lock acquired: {}", stamp);
    try {
      T result = optimisticReadAction.get();
      LOGGER.info("Optimistic read action executed successfully, validating lock: {}", stamp);
      if (!lock.validate(stamp)) {
        LOGGER.info("Optimistic read lock validation failed, upgrading to write lock");
        return withWriteLock(lock, writeAction);
      }
      LOGGER.info("Optimistic read lock validation succeeded");
      return result;
    } catch (Exception e) {
      LOGGER.error("Exception during optimistic read action", e);
      throw e;
    }
  }

  private static long tryAcquireWriteLock(StampedLock lock, long timeoutMs) throws InterruptedException {
    long stamp = 0;
    long deadline = System.currentTimeMillis() + timeoutMs;
    while (stamp == 0 && System.currentTimeMillis() < deadline) {
      stamp = lock.tryWriteLock(100, TimeUnit.MILLISECONDS);  // Try acquiring the lock with a timeout
      if (stamp == 0) {
        LOGGER.info("Retrying to acquire write lock...");
      }
    }
    if (stamp == 0) {
      LOGGER.error("Failed to acquire write lock within timeout");
    }
    return stamp;
  }
}
