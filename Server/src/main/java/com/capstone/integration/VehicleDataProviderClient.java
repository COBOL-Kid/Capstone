package com.capstone.integration;

import com.capstone.configuration.VehicleDataProviderConfig;
import com.capstone.domain.VinNotFoundException;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class VehicleDataProviderClient {

  private final RestTemplate restTemplate;
  private final VehicleDataProviderConfig providerConfig;

  public VehicleDataProviderClient(
      RestTemplate restTemplate, VehicleDataProviderConfig providerConfig) {
    this.restTemplate = restTemplate;
    this.providerConfig = providerConfig;
  }

  public VinDecodeResponse decodeVin(String vin) {
    return getAutoDev("/vin/{vin}", vin, VinDecodeResponse.class, true);
  }

  public RepairEstimatesResponse getRepairEstimates(String vin) {
    return getVehicleDatabases("/repair-estimates/{vin}", vin, RepairEstimatesResponse.class);
  }

  public RepairCostResponse getRepairCosts(String vin) {
    return getVehicleDatabases("/vehicle-repairs/v2/{vin}", vin, RepairCostResponse.class);
  }

  public VehicleRecallsResponse getRecalls(String vin) {
    return getVehicleDatabases("/vehicle-recalls/{vin}", vin, VehicleRecallsResponse.class);
  }

  public OwnerManualResponse getOwnerManual(String vin) {
    return getVehicleDatabases("/owner-manual/{vin}", vin, OwnerManualResponse.class);
  }

  public VehiclePhotosResponse getPhotos(String vin) {
    return getAutoDev("/photos/{vin}", vin, VehiclePhotosResponse.class, false);
  }

  private <T> T getAutoDev(
      String path, String vin, Class<T> responseType, boolean notFoundMeansInvalidVin) {
    return get(
        providerConfig.getAutoDevBaseUrl(),
        path,
        vin,
        responseType,
        providerConfig.getAutoDevApiKey(),
        providerConfig.getAutoDevApiKeyHeader(),
        notFoundMeansInvalidVin);
  }

  private <T> T getVehicleDatabases(String path, String vin, Class<T> responseType) {
    return get(
        providerConfig.getVehicleDatabasesBaseUrl(),
        path,
        vin,
        responseType,
        providerConfig.getVehicleDatabasesApiKey(),
        providerConfig.getVehicleDatabasesApiKeyHeader(),
        false);
  }

  private <T> T get(
      String baseUrl,
      String path,
      String vin,
      Class<T> responseType,
      String apiKey,
      String apiKeyHeader,
      boolean notFoundMeansInvalidVin) {
    String url =
        UriComponentsBuilder.fromUriString(baseUrl).path(path).buildAndExpand(vin).toUriString();
    try {
      ResponseEntity<T> response =
          restTemplate.exchange(
              url, HttpMethod.GET, new HttpEntity<>(headers(apiKey, apiKeyHeader)), responseType);
      T body = response.getBody();
      if (body == null) {
        throw new IllegalStateException("Vehicle data provider returned an empty response");
      }
      return body;
    } catch (HttpStatusCodeException ex) {
      if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
        if (notFoundMeansInvalidVin) {
          throw new VinNotFoundException();
        }
        return null;
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
}
