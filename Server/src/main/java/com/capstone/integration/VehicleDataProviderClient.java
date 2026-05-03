package com.capstone.integration;

import com.capstone.configuration.VehicleDataProviderConfig;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class VehicleDataProviderClient {

    private final RestTemplate restTemplate;
    private final VehicleDataProviderConfig providerConfig;

    public VehicleDataProviderClient(RestTemplate restTemplate, VehicleDataProviderConfig providerConfig) {
        this.restTemplate = restTemplate;
        this.providerConfig = providerConfig;
    }

    public VinDecodeResponse decodeVin(String vin) {
        return get("/vin/{vin}", vin, VinDecodeResponse.class);
    }

    public MaintenanceScheduleResponse getMaintenanceSchedule(String vin) {
        return get("/vehicle-maintenance/v4/{vin}", vin, MaintenanceScheduleResponse.class);
    }

    public RepairCostResponse getRepairCosts(String vin) {
        return get("/vehicle-repairs/v2/{vin}", vin, RepairCostResponse.class);
    }

    public RecallProviderResponse getRecalls(String vin) {
        return get(providerConfig.getVehicleDataRecallBaseUrl(), "/openrecalls/{vin}", vin, RecallProviderResponse.class,
                providerConfig.getVehicleDataRecallApiKey(), providerConfig.getVehicleDataRecallApiKeyHeader());
    }

    public OwnerManualResponse getOwnerManual(String vin) {
        return get("/owner-manual/{vin}", vin, OwnerManualResponse.class);
    }

    public VehiclePhotosResponse getPhotos(String vin) {
        return get("/photos/{vin}", vin, VehiclePhotosResponse.class);
    }

    private <T> T get(String path, String vin, Class<T> responseType) {
        return get(providerConfig.getVehicleDataBaseUrl(), path, vin, responseType,
                providerConfig.getVehicleDataApiKey(), providerConfig.getVehicleDataApiKeyHeader());
    }

    private <T> T get(String baseUrl, String path, String vin, Class<T> responseType, String apiKey,
                      String apiKeyHeader) {
        String url = UriComponentsBuilder.fromUriString(baseUrl).path(path).buildAndExpand(vin).toUriString();
        ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET,
                new HttpEntity<>(headers(apiKey, apiKeyHeader)), responseType);
        T body = response.getBody();
        if (body == null) {
            throw new IllegalStateException("Vehicle data provider returned an empty response");
        }
        return body;
    }

    private HttpHeaders headers(String apiKey, String apiKeyHeader) {
        HttpHeaders headers = new HttpHeaders();
        if (apiKey != null && !apiKey.isBlank()) {
            headers.set(apiKeyHeader, apiKey);
        }
        return headers;
    }
}
