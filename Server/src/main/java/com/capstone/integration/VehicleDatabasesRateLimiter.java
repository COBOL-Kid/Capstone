package com.capstone.integration;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.locks.LockSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VehicleDatabasesRateLimiter {

  private static final Logger log = LoggerFactory.getLogger(VehicleDatabasesRateLimiter.class);
  private static final long WINDOW_MICROS = 1_000_000L;

  private final int maxPermits;
  private final Object lock = new Object();
  private final Deque<Long> recentAcquireMicros = new ArrayDeque<>();

  public VehicleDatabasesRateLimiter(double maxRequestsPerSecond) {
    if (maxRequestsPerSecond <= 0) {
      this.maxPermits = 0;
    } else {
      this.maxPermits = Math.max(1, (int) Math.floor(maxRequestsPerSecond));
    }
  }

  public void acquire(String operation) {
    if (maxPermits <= 0) {
      return;
    }
    while (true) {
      long waitMicros = 0;
      synchronized (lock) {
        long nowMicros = System.nanoTime() / 1000;
        pruneExpired(nowMicros);
        if (recentAcquireMicros.size() < maxPermits) {
          recentAcquireMicros.addLast(nowMicros);
          return;
        }
        long oldestMicros = recentAcquireMicros.peekFirst();
        waitMicros = WINDOW_MICROS - (nowMicros - oldestMicros) + 1;
      }
      if (waitMicros > 0) {
        log.debug(
            "Vehicle Databases rate limit waiting operation={} waitMs={}",
            operation,
            waitMicros / 1000);
        LockSupport.parkNanos(waitMicros * 1000);
      }
    }
  }

  private void pruneExpired(long nowMicros) {
    while (!recentAcquireMicros.isEmpty()
        && nowMicros - recentAcquireMicros.peekFirst() >= WINDOW_MICROS) {
      recentAcquireMicros.removeFirst();
    }
  }
}
