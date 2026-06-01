package com.capstone.integration;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class VehicleDataProviderRequestMetrics {

  private static final ScopedValue<VehicleDataProviderPrefetchMetrics> PREFETCH =
      ScopedValue.newInstance();

  private final AtomicInteger globalInFlight = new AtomicInteger();

  public ScopedValue.Carrier bindPrefetchMetrics(VehicleDataProviderPrefetchMetrics prefetch) {
    return ScopedValue.where(PREFETCH, prefetch);
  }

  public void requestStarted() {
    globalInFlight.incrementAndGet();
    if (PREFETCH.isBound()) {
      PREFETCH.get().requestStarted();
    }
  }

  public void requestFinished() {
    globalInFlight.decrementAndGet();
    if (PREFETCH.isBound()) {
      PREFETCH.get().requestFinished();
    }
  }

  public int globalInFlight() {
    return globalInFlight.get();
  }
}
