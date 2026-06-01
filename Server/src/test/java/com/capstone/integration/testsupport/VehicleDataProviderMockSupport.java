package com.capstone.integration.testsupport;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

public final class VehicleDataProviderMockSupport {

  public static final String AUTO_DEV_BASE = "https://api.auto.dev";
  public static final String VDB_BASE = "https://api.vehicledatabases.com";

  private VehicleDataProviderMockSupport() {}

  public static void expectNewVinOnboardingCalls(
      MockRestServiceServer server,
      String vin,
      String year,
      String make,
      String model,
      long delayMs,
      AtomicInteger inFlight,
      AtomicInteger maxInFlight,
      CopyOnWriteArrayList<Long> requestStarts,
      AtomicInteger vdbInFlight,
      AtomicInteger vdbMaxInFlight,
      CopyOnWriteArrayList<Long> vdbRequestStarts) {
    server
        .expect(requestTo(AUTO_DEV_BASE + "/vin/" + vin))
        .andRespond(
            delayed(
                VehicleDataFixtures.read("autodev-vin-decode.json"),
                delayMs,
                inFlight,
                maxInFlight,
                requestStarts,
                "decodeVin"));
    server
        .expect(requestTo(AUTO_DEV_BASE + "/photos/" + vin))
        .andRespond(
            delayed(
                VehicleDataFixtures.read("autodev-photos.json"),
                delayMs,
                inFlight,
                maxInFlight,
                requestStarts,
                "getPhotos"));
    server
        .expect(requestTo(VDB_BASE + "/owner-manual/" + vin))
        .andRespond(
            delayedVdb(
                VehicleDataFixtures.read("vdb-owner-manual.json"),
                delayMs,
                inFlight,
                maxInFlight,
                requestStarts,
                vdbInFlight,
                vdbMaxInFlight,
                vdbRequestStarts,
                "getOwnerManual"));
    server
        .expect(requestTo(VDB_BASE + "/repair-estimates/" + vin))
        .andRespond(
            delayedVdb(
                VehicleDataFixtures.read("vdb-repair-estimates.json"),
                delayMs,
                inFlight,
                maxInFlight,
                requestStarts,
                vdbInFlight,
                vdbMaxInFlight,
                vdbRequestStarts,
                "getRepairEstimates"));
    server
        .expect(requestTo(VDB_BASE + "/vehicle-repairs/v2/" + vin))
        .andRespond(
            delayedVdb(
                VehicleDataFixtures.read("vdb-repair-costs.json"),
                delayMs,
                inFlight,
                maxInFlight,
                requestStarts,
                vdbInFlight,
                vdbMaxInFlight,
                vdbRequestStarts,
                "getRepairCosts"));
    server
        .expect(requestTo(VDB_BASE + "/vehicle-recalls/" + vin))
        .andRespond(
            delayedVdb(
                VehicleDataFixtures.read("vdb-vehicle-recalls.json"),
                delayMs,
                inFlight,
                maxInFlight,
                requestStarts,
                vdbInFlight,
                vdbMaxInFlight,
                vdbRequestStarts,
                "getRecalls"));
    server
        .expect(requestTo(VDB_BASE + "/vehicle-warranty/" + year + "/" + make + "/" + model))
        .andRespond(
            delayedVdb(
                VehicleDataFixtures.read("vdb-vehicle-warranty.json"),
                delayMs,
                inFlight,
                maxInFlight,
                requestStarts,
                vdbInFlight,
                vdbMaxInFlight,
                vdbRequestStarts,
                "getVehicleWarranty"));
  }

  public static void expectDecodeWithRateLimitedPrefetch(MockRestServiceServer server, String vin) {
    server
        .expect(requestTo(AUTO_DEV_BASE + "/vin/" + vin))
        .andRespond(
            withSuccess(
                VehicleDataFixtures.read("autodev-vin-decode-rate429.json"),
                MediaType.APPLICATION_JSON));
    server
        .expect(requestTo(AUTO_DEV_BASE + "/photos/" + vin))
        .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).headers(rateLimitHeaders()));
    server
        .expect(requestTo(VDB_BASE + "/owner-manual/" + vin))
        .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).headers(rateLimitHeaders()));
    server
        .expect(requestTo(VDB_BASE + "/repair-estimates/" + vin))
        .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).headers(rateLimitHeaders()));
    server
        .expect(requestTo(VDB_BASE + "/vehicle-repairs/v2/" + vin))
        .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).headers(rateLimitHeaders()));
    server
        .expect(requestTo(VDB_BASE + "/vehicle-recalls/" + vin))
        .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).headers(rateLimitHeaders()));
    server
        .expect(requestTo(VDB_BASE + "/vehicle-warranty/2014/Dodge/Durango"))
        .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).headers(rateLimitHeaders()));
  }

  private static HttpHeaders rateLimitHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.RETRY_AFTER, "60");
    return headers;
  }

  private static DelayedJsonResponse delayed(
      String json,
      long delayMs,
      AtomicInteger inFlight,
      AtomicInteger maxInFlight,
      CopyOnWriteArrayList<Long> requestStarts,
      String operation) {
    return new DelayedJsonResponse(json, delayMs, inFlight, maxInFlight, requestStarts, operation);
  }

  private static DelayedJsonResponse delayedVdb(
      String json,
      long delayMs,
      AtomicInteger inFlight,
      AtomicInteger maxInFlight,
      CopyOnWriteArrayList<Long> requestStarts,
      AtomicInteger vdbInFlight,
      AtomicInteger vdbMaxInFlight,
      CopyOnWriteArrayList<Long> vdbRequestStarts,
      String operation) {
    return new DelayedJsonResponse(
        json,
        delayMs,
        inFlight,
        maxInFlight,
        requestStarts,
        vdbInFlight,
        vdbMaxInFlight,
        vdbRequestStarts,
        operation);
  }
}
