package com.capstone.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class VehicleDataProviderRequestMetricsTest {

  @Test
  void prefetchPeaksAreIsolatedAcrossConcurrentScopes() throws InterruptedException {
    VehicleDataProviderRequestMetrics metrics = new VehicleDataProviderRequestMetrics();
    VehicleDataProviderPrefetchMetrics prefetchA = new VehicleDataProviderPrefetchMetrics();
    VehicleDataProviderPrefetchMetrics prefetchB = new VehicleDataProviderPrefetchMetrics();
    ScopedValue.Carrier scopeA = metrics.bindPrefetchMetrics(prefetchA);
    ScopedValue.Carrier scopeB = metrics.bindPrefetchMetrics(prefetchB);
    CountDownLatch bothStarted = new CountDownLatch(2);
    CountDownLatch releaseA = new CountDownLatch(1);

    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      executor.submit(
          () ->
              scopeA.call(
                  () -> {
                    metrics.requestStarted();
                    bothStarted.countDown();
                    try {
                      releaseA.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException ex) {
                      Thread.currentThread().interrupt();
                    } finally {
                      metrics.requestFinished();
                    }
                    return null;
                  }));

      executor.submit(
          () ->
              scopeB.call(
                  () -> {
                    bothStarted.countDown();
                    metrics.requestStarted();
                    metrics.requestFinished();
                    return null;
                  }));

      assertTrue(bothStarted.await(5, TimeUnit.SECONDS));
      assertEquals(1, prefetchB.maxInFlight());
      assertEquals(1, prefetchA.maxInFlight());

      releaseA.countDown();
    }
  }
}
