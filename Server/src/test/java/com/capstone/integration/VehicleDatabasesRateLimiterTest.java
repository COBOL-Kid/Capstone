package com.capstone.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class VehicleDatabasesRateLimiterTest {

  @Test
  void shouldNotWaitWhenDisabled() {
    VehicleDatabasesRateLimiter limiter = new VehicleDatabasesRateLimiter(0);
    long startNanos = System.nanoTime();
    for (int i = 0; i < 20; i++) {
      limiter.acquire("op" + i);
    }
    long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
    assertTrue(elapsedMs < 200, "disabled limiter should not block, took " + elapsedMs + "ms");
  }

  @Test
  void shouldSpaceAcquisitionsAtConfiguredRate() throws InterruptedException {
    VehicleDatabasesRateLimiter limiter = new VehicleDatabasesRateLimiter(2.0);
    int callCount = 5;
    CopyOnWriteArrayList<Long> acquireTimesMs = new CopyOnWriteArrayList<>();
    CountDownLatch startGate = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(callCount);

    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      for (int i = 0; i < callCount; i++) {
        executor.submit(
            () -> {
              try {
                startGate.await();
                limiter.acquire("parallel");
                acquireTimesMs.add(System.currentTimeMillis());
              } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
              } finally {
                done.countDown();
              }
            });
      }
      long startMs = System.currentTimeMillis();
      startGate.countDown();
      assertTrue(done.await(10, TimeUnit.SECONDS), "acquisitions did not finish in time");
      long elapsedMs = System.currentTimeMillis() - startMs;

      List<Long> sorted = new ArrayList<>(acquireTimesMs);
      Collections.sort(sorted);
      assertEquals(callCount, sorted.size());
      assertTrue(
          elapsedMs >= 1_800,
          "five calls at 2/sec should take at least ~2s, took " + elapsedMs + "ms");
      assertNoMoreThanTwoInAnyOneSecondWindow(sorted);
    }
  }

  private static void assertNoMoreThanTwoInAnyOneSecondWindow(List<Long> sortedAcquireTimesMs) {
    for (int i = 0; i < sortedAcquireTimesMs.size(); i++) {
      long windowStart = sortedAcquireTimesMs.get(i);
      int countInWindow = 0;
      for (long t : sortedAcquireTimesMs) {
        if (t >= windowStart && t < windowStart + 1000) {
          countInWindow++;
        }
      }
      assertTrue(
          countInWindow <= 2,
          "expected at most 2 acquisitions per 1s window starting at "
              + windowStart
              + ", saw "
              + countInWindow
              + " times="
              + sortedAcquireTimesMs);
    }
  }
}
