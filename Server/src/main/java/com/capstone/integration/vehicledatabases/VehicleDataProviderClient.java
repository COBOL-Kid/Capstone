package com.capstone.integration.vehicledatabases;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.capstone.configuration.WebClientConfig;

@Component
public class VehicleDataProviderClient {

    private final RestTemplate restTemplate;
    private final WebClientConfig webClientConfig;

    public VehicleDataProviderClient(RestTemplate restTemplate, WebClientConfig webClientConfig) {
        this.restTemplate = restTemplate;
        this.webClientConfig = webClientConfig;
    }

    public VinDecodeResponse decodeVin(String vin) {
        return get("/vin-decode/{vin}", vin, VinDecodeResponse.class);
    }

    public MaintenanceScheduleResponse getMaintenanceSchedule(String vin) {
        return get("/vehicle-maintenance/v4/{vin}", vin, MaintenanceScheduleResponse.class);
    }

    public RepairCostResponse getRepairCosts(String vin) {
        return get("/vehicle-repairs/v2/{vin}", vin, RepairCostResponse.class);
    }

    public RecallResponse getRecalls(String vin) {
        return get("/vehicle-recalls/{vin}", vin, RecallResponse.class);
    }

    public OwnerManualResponse getOwnerManual(String vin) {
        return get("/owner-manual/{vin}", vin, OwnerManualResponse.class);
    }

    private <T> T get(String path, String vin, Class<T> responseType) {
        String url = UriComponentsBuilder.fromUriString(webClientConfig.getVehicleDataBaseUrl())
                .path(path)
                .buildAndExpand(vin)
                .toUriString();
        ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers()),
                responseType);
        T body = response.getBody();
        if (body == null) {
            throw new IllegalStateException("Vehicle data provider returned an empty response");
        }
        return body;
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        if (!webClientConfig.getVehicleDataApiKey().isBlank()) {
            headers.set(webClientConfig.getVehicleDataApiKeyHeader(), webClientConfig.getVehicleDataApiKey());
        }
        return headers;
    }
}