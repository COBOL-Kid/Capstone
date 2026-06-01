package com.capstone.integration;

import java.util.concurrent.atomic.AtomicInteger;

public final class VehicleDataProviderPrefetchMetrics {

  private final AtomicInteger inFlight = new AtomicInteger();
  private final AtomicInteger maxInFlight = new AtomicInteger();

  void requestStarted() {
    int current = inFlight.incrementAndGet();
    maxInFlight.accumulateAndGet(current, Math::max);
  }

  void requestFinished() {
    inFlight.decrementAndGet();
  }

  public int maxInFlight() {
    return maxInFlight.get();
  }
}
