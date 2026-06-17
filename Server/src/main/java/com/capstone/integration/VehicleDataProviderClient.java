package com.capstone.integration;

import com.capstone.domain.VehicleDatabasesPathNormalizer;
import com.capstone.domain.VinNotFoundException;
import com.capstone.logging.LogRedaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class VehicleDataProviderClient {

  private static final Logger log = LoggerFactory.getLogger(VehicleDataProviderClient.class);
  private static final int MAX_ERROR_BODY_LOG_CHARS = 500;

  private final RestTemplate restTemplate;
  private final VehicleDatabasesRateLimiter vehicleDatabasesRateLimiter;
  private final String autoDevBaseUrl;
  private final String autoDevApiKey;
  private final String autoDevApiKeyHeader;
  private final String vehicleDatabasesBaseUrl;
  private final String vehicleDatabasesApiKey;
  private final String vehicleDatabasesApiKeyHeader;

  public VehicleDataProviderClient(
      RestTemplate restTemplate,
      VehicleDatabasesRateLimiter vehicleDatabasesRateLimiter,
      @Value("${vehicle-data.autodev.base-url}") String autoDevBaseUrl,
      @Value("${vehicle-data.autodev.api-key}") String autoDevApiKey,
      @Value("${vehicle-data.autodev.api-key-header}") String autoDevApiKeyHeader,
      @Value("${vehicle-data.vehicle-databases.base-url}") String vehicleDatabasesBaseUrl,
      @Value("${vehicle-data.vehicle-databases.api-key}") String vehicleDatabasesApiKey,
      @Value("${vehicle-data.vehicle-databases.api-key-header}")
          String vehicleDatabasesApiKeyHeader) {
    this.restTemplate = restTemplate;
    this.vehicleDatabasesRateLimiter = vehicleDatabasesRateLimiter;
    this.autoDevBaseUrl = autoDevBaseUrl;
    this.autoDevApiKey = autoDevApiKey;
    this.autoDevApiKeyHeader = autoDevApiKeyHeader;
    this.vehicleDatabasesBaseUrl = vehicleDatabasesBaseUrl;
    this.vehicleDatabasesApiKey = vehicleDatabasesApiKey;
    this.vehicleDatabasesApiKeyHeader = vehicleDatabasesApiKeyHeader;
  }

  public VinDecodeResponse decodeVin(String vin) {
    return getAutoDev("decodeVin", "/vin/{vin}", vin, VinDecodeResponse.class, true);
  }

  public RepairEstimatesResponse getRepairEstimates(String vin) {
    return getVehicleDatabases(
        "getRepairEstimates", "/repair-estimates/{vin}", vin, RepairEstimatesResponse.class);
  }

  public RepairEstimatesVinProbeResult probeRepairEstimatesByVin(String vin) {
    vehicleDatabasesRateLimiter.acquire("getRepairEstimates");
    String url =
        UriComponentsBuilder.fromUriString(vehicleDatabasesBaseUrl)
            .path("/repair-estimates/{vin}")
            .buildAndExpand(vin)
            .toUriString();
    try {
      RepairEstimatesResponse response =
          executeGet(
              "vehicle-databases",
              "getRepairEstimates",
              url,
              RepairEstimatesResponse.class,
              headers(vehicleDatabasesApiKey, vehicleDatabasesApiKeyHeader),
              false,
              false,
              LogRedaction.maskVin(vin));
      return new RepairEstimatesVinProbeResult.Found(response);
    } catch (HttpStatusCodeException ex) {
      if (ex.getStatusCode().value() == HttpStatus.BAD_REQUEST.value()) {
        return new RepairEstimatesVinProbeResult.TrimSelectionRequired();
      }
      throw ex;
    }
  }

  public TrimOptionsResponse getTrimOptions(String year, String make, String model) {
    String[] ymm = normalizeYmm(year, make, model);
    return getVehicleDatabasesByPathSegments(
        "getTrimOptions",
        "/repair-estimates/options/trim/{year}/{make}/{model}",
        TrimOptionsResponse.class,
        ymm[0],
        ymm[1],
        ymm[2]);
  }

  public RepairEstimatesResponse getRepairEstimates(
      String year, String make, String model, String trim) {
    String[] ymmt = normalizeYmmt(year, make, model, trim);
    return getVehicleDatabasesByPathSegments(
        "getRepairEstimatesByYearMakeModelTrim",
        "/repair-estimates/{year}/{make}/{model}/{trim}",
        RepairEstimatesResponse.class,
        ymmt[0],
        ymmt[1],
        ymmt[2],
        ymmt[3]);
  }

  public OwnerManualResponse getOwnerManual(String year, String make, String model) {
    String[] ymm = normalizeYmm(year, make, model);
    return getVehicleDatabasesByPathSegments(
        "getOwnerManualByYearMakeModel",
        "/owner-manual/{year}/{make}/{model}",
        OwnerManualResponse.class,
        ymm[0],
        ymm[1],
        ymm[2]);
  }

  public RepairCostResponse getRepairCosts(String year, String make, String model) {
    String[] ymm = normalizeYmm(year, make, model);
    return getVehicleDatabasesByPathSegments(
        "getRepairCostsByYearMakeModel",
        "/vehicle-repairs/v2/{year}/{make}/{model}",
        RepairCostResponse.class,
        ymm[0],
        ymm[1],
        ymm[2]);
  }

  public VehicleRecallsResponse getRecalls(String year, String make, String model) {
    String[] ymm = normalizeYmm(year, make, model);
    return getVehicleDatabasesByPathSegments(
        "getRecallsByYearMakeModel",
        "/vehicle-recalls/{year}/{make}/{model}",
        VehicleRecallsResponse.class,
        ymm[0],
        ymm[1],
        ymm[2]);
  }

  public RepairCostResponse getRepairCosts(String vin) {
    return getVehicleDatabases(
        "getRepairCosts", "/vehicle-repairs/v2/{vin}", vin, RepairCostResponse.class);
  }

  public VehicleRecallsResponse getRecalls(String vin) {
    return getVehicleDatabases(
        "getRecalls", "/vehicle-recalls/{vin}", vin, VehicleRecallsResponse.class);
  }

  public OwnerManualResponse getOwnerManual(String vin) {
    return getVehicleDatabases(
        "getOwnerManual", "/owner-manual/{vin}", vin, OwnerManualResponse.class);
  }

  public VehicleWarrantyResponse getVehicleWarranty(String year, String make, String model) {
    String[] ymm = normalizeYmm(year, make, model);
    return getVehicleDatabasesByPathSegments(
        "getVehicleWarranty",
        "/vehicle-warranty/{year}/{make}/{model}",
        VehicleWarrantyResponse.class,
        ymm[0],
        ymm[1],
        ymm[2]);
  }

  public VehiclePhotosResponse getPhotos(String vin) {
    return getAutoDev("getPhotos", "/photos/{vin}", vin, VehiclePhotosResponse.class, false);
  }

  private <T> T getAutoDev(
      String operation,
      String path,
      String vin,
      Class<T> responseType,
      boolean notFoundMeansInvalidVin) {
    return get(
        "autodev",
        operation,
        autoDevBaseUrl,
        path,
        vin,
        responseType,
        autoDevApiKey,
        autoDevApiKeyHeader,
        notFoundMeansInvalidVin,
        false);
  }

  private <T> T getVehicleDatabases(
      String operation, String path, String vin, Class<T> responseType) {
    vehicleDatabasesRateLimiter.acquire(operation);
    return get(
        "vehicle-databases",
        operation,
        vehicleDatabasesBaseUrl,
        path,
        vin,
        responseType,
        vehicleDatabasesApiKey,
        vehicleDatabasesApiKeyHeader,
        false,
        true);
  }

  private <T> T getVehicleDatabasesByPathSegments(
      String operation, String path, Class<T> responseType, String... pathSegments) {
    vehicleDatabasesRateLimiter.acquire(operation);
    String url =
        UriComponentsBuilder.fromUriString(vehicleDatabasesBaseUrl)
            .path(path)
            .buildAndExpand((Object[]) pathSegments)
            .toUriString();
    return executeGet(
        "vehicle-databases",
        operation,
        url,
        responseType,
        headers(vehicleDatabasesApiKey, vehicleDatabasesApiKeyHeader),
        false,
        true,
        String.join("/", pathSegments));
  }

  private <T> T get(
      String provider,
      String operation,
      String baseUrl,
      String path,
      String vin,
      Class<T> responseType,
      String apiKey,
      String apiKeyHeader,
      boolean notFoundMeansInvalidVin,
      boolean allowBadRequestAsEmpty) {
    String url =
        UriComponentsBuilder.fromUriString(baseUrl).path(path).buildAndExpand(vin).toUriString();
    return executeGet(
        provider,
        operation,
        url,
        responseType,
        headers(apiKey, apiKeyHeader),
        notFoundMeansInvalidVin,
        allowBadRequestAsEmpty,
        LogRedaction.maskVin(vin));
  }

  private <T> T executeGet(
      String provider,
      String operation,
      String url,
      Class<T> responseType,
      HttpHeaders headers,
      boolean notFoundMeansInvalidVin,
      boolean allowBadRequestAsEmpty,
      String target) {
    log.debug(
        "Vehicle data request starting provider={} operation={} target={}",
        provider,
        operation,
        target);
    long startNanos = System.nanoTime();
    try {
      ResponseEntity<T> response =
          restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), responseType);
      T body = response.getBody();
      if (body == null) {
        log.error(
            "Vehicle data empty body provider={} operation={} target={} durationMs={}",
            provider,
            operation,
            target,
            durationMs(startNanos));
        throw new IllegalStateException("Vehicle data provider returned an empty response");
      }
      log.debug(
          "Vehicle data request succeeded provider={} operation={} target={} status={} durationMs={}",
          provider,
          operation,
          target,
          response.getStatusCode().value(),
          durationMs(startNanos));
      return body;
    } catch (HttpStatusCodeException ex) {
      long durationMs = durationMs(startNanos);
      HttpStatusCode status = ex.getStatusCode();
      if (status.value() == HttpStatus.NOT_FOUND.value()) {
        log.info(
            "Vehicle data not found provider={} operation={} target={} durationMs={}",
            provider,
            operation,
            target,
            durationMs);
        if (notFoundMeansInvalidVin) {
          throw new VinNotFoundException();
        }
        return null;
      }
      if (allowBadRequestAsEmpty && status.value() == HttpStatus.BAD_REQUEST.value()) {
        log.warn(
            "Vehicle data bad request treated as empty provider={} operation={} target={} durationMs={} body={}",
            provider,
            operation,
            target,
            durationMs,
            truncate(ex.getResponseBodyAsString()));
        return null;
      }
      if (status.value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
        log.warn(
            "Vehicle data rate limited provider={} operation={} target={} durationMs={} retryAfter={}",
            provider,
            operation,
            target,
            durationMs,
            ex.getResponseHeaders() != null
                ? ex.getResponseHeaders().getFirst(HttpHeaders.RETRY_AFTER)
                : null);
      } else {
        log.warn(
            "Vehicle data HTTP error provider={} operation={} target={} status={} durationMs={} body={}",
            provider,
            operation,
            target,
            status.value(),
            durationMs,
            truncate(ex.getResponseBodyAsString()));
      }
      throw ex;
    }
  }

  private HttpHeaders headers(String apiKey, String apiKeyHeader) {
    HttpHeaders headers = new HttpHeaders();
    if (apiKey != null && !apiKey.isBlank()) {
      headers.set(apiKeyHeader, apiKey);
    }
    return headers;
  }

  private static long durationMs(long startNanos) {
    return (System.nanoTime() - startNanos) / 1_000_000L;
  }

  private static String truncate(String body) {
    if (body == null || body.isBlank()) {
      return "";
    }
    if (body.length() <= MAX_ERROR_BODY_LOG_CHARS) {
      return body;
    }
    return body.substring(0, MAX_ERROR_BODY_LOG_CHARS) + "...";
  }

  private static String[] normalizeYmm(String year, String make, String model) {
    return new String[] {
      VehicleDatabasesPathNormalizer.normalizeYear(year),
      VehicleDatabasesPathNormalizer.normalizeMake(make),
      VehicleDatabasesPathNormalizer.normalizeModel(model)
    };
  }

  private static String[] normalizeYmmt(String year, String make, String model, String trim) {
    return new String[] {
      VehicleDatabasesPathNormalizer.normalizeYear(year),
      VehicleDatabasesPathNormalizer.normalizeMake(make),
      VehicleDatabasesPathNormalizer.normalizeModel(model),
      VehicleDatabasesPathNormalizer.normalizeTrim(trim)
    };
  }
}
