package com.iluwatar.ratelimiter.infrastructure.datastructures;

import java.util.concurrent.ConcurrentLinkedDeque;

public class LockFreeTimestampQueue {

  private final ConcurrentLinkedDeque<Long> timestamps = new ConcurrentLinkedDeque<>();

  public void offer(long timestamp) {
    timestamps.offer(timestamp);
  }

  public long poll(long threshold) {
    Long first = timestamps.peekFirst();
    if (first == null || first > threshold) {
      return -1;
    }
    return timestamps.pollFirst();
  }

  public int size() {
    return timestamps.size();
  }

  public long peekFirst() {
    Long first = timestamps.peekFirst();
    return (first != null) ? first : -1;
  }
}