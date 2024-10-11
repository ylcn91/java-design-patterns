package com.iluwatar.ratelimiter.infrastructure.datastructures;

import java.util.concurrent.atomic.AtomicReference;

public class LockFreeTimestampQueue {
  private final AtomicReference<Node> head = new AtomicReference<>(null);
  private final AtomicReference<Node> tail = new AtomicReference<>(null);

  private static class Node {
    final long timestamp;
    final AtomicReference<Node> next = new AtomicReference<>(null);
    Node(long timestamp) {
      this.timestamp = timestamp;
    }
  }

  public void offer(long timestamp) {
    Node newNode = new Node(timestamp);
    while (true) {
      Node t = tail.get();
      Node h = head.get();
      if (t == null) {
        if (head.compareAndSet(null, newNode)) {
          tail.set(newNode);
          return;
        }
      } else {
        if (tail.compareAndSet(t, newNode)) {
          t.next.set(newNode);
          return;
        }
      }
    }
  }

  public long poll(long threshold) {
    while (true) {
      Node h = head.get();
      if (h == null) return -1;
      if (h.timestamp > threshold) return -1;
      Node next = h.next.get();
      if (head.compareAndSet(h, next)) {
        return h.timestamp;
      }
    }
  }

  public int size() {
    int count = 0;
    Node current = head.get();
    while (current != null) {
      count++;
      current = current.next.get();
    }
    return count;
  }

  public long peekFirst() {
    Node h = head.get();
    return h != null ? h.timestamp : -1;
  }
}