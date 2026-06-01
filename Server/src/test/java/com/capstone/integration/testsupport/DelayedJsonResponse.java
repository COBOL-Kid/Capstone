package com.capstone.integration.testsupport;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.test.web.client.response.MockRestResponseCreators;

public final class DelayedJsonResponse implements ResponseCreator {

  private final String jsonBody;
  private final long delayMs;
  private final AtomicInteger inFlight;
  private final AtomicInteger maxInFlight;
  private final CopyOnWriteArrayList<Long> requestStartEpochMs;
  private final AtomicInteger providerInFlight;
  private final AtomicInteger providerMaxInFlight;
  private final CopyOnWriteArrayList<Long> providerRequestStartEpochMs;
  private final String operationLabel;

  public DelayedJsonResponse(
      String jsonBody,
      long delayMs,
      AtomicInteger inFlight,
      AtomicInteger maxInFlight,
      CopyOnWriteArrayList<Long> requestStartEpochMs,
      String operationLabel) {
    this(
        jsonBody,
        delayMs,
        inFlight,
        maxInFlight,
        requestStartEpochMs,
        null,
        null,
        null,
        operationLabel);
  }

  public DelayedJsonResponse(
      String jsonBody,
      long delayMs,
      AtomicInteger inFlight,
      AtomicInteger maxInFlight,
      CopyOnWriteArrayList<Long> requestStartEpochMs,
      AtomicInteger providerInFlight,
      AtomicInteger providerMaxInFlight,
      CopyOnWriteArrayList<Long> providerRequestStartEpochMs,
      String operationLabel) {
    this.jsonBody = jsonBody;
    this.delayMs = delayMs;
    this.inFlight = inFlight;
    this.maxInFlight = maxInFlight;
    this.requestStartEpochMs = requestStartEpochMs;
    this.providerInFlight = providerInFlight;
    this.providerMaxInFlight = providerMaxInFlight;
    this.providerRequestStartEpochMs = providerRequestStartEpochMs;
    this.operationLabel = operationLabel;
  }

  @Override
  public ClientHttpResponse createResponse(ClientHttpRequest request) throws IOException {
    long nowMs = System.currentTimeMillis();
    requestStartEpochMs.add(nowMs);
    if (providerRequestStartEpochMs != null) {
      providerRequestStartEpochMs.add(nowMs);
    }
    int current = inFlight.incrementAndGet();
    maxInFlight.accumulateAndGet(current, Math::max);
    if (providerInFlight != null) {
      int providerCurrent = providerInFlight.incrementAndGet();
      providerMaxInFlight.accumulateAndGet(providerCurrent, Math::max);
    }
    try {
      if (delayMs > 0) {
        Thread.sleep(delayMs);
      }
      return MockRestResponseCreators.withSuccess(jsonBody, MediaType.APPLICATION_JSON)
          .createResponse(request);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted during delayed response for " + operationLabel, ex);
    } finally {
      inFlight.decrementAndGet();
      if (providerInFlight != null) {
        providerInFlight.decrementAndGet();
      }
    }
  }
}
