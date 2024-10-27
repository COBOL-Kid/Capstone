package com.capstone.controllers;

import java.util.List;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.capstone.configuration.WebClientConfig;
import com.capstone.models.Maintenance;
import com.capstone.models.MaintenanceResponse;
import com.capstone.models.VehicleInfo;
import com.capstone.models.VinResponse;

@RestController
@RequestMapping("/api/external/")
public class CarWrapperController {

    private final RestTemplate restTemplate;
    private final WebClientConfig webClientConfig;

    public CarWrapperController(RestTemplate restTemplate, WebClientConfig webClientConfig) {
        this.restTemplate = restTemplate;
        this.webClientConfig = webClientConfig;
    }

    @GetMapping("/find_vin/{vin}")
    public VehicleInfo getVehicleInfo(@PathVariable String vin) {
        if (vin == null) {
            throw new IllegalArgumentException("VIN cannot be null");
        }
        String url = String.format("http://api.carmd.com/v3.0/decode?vin=%s", vin);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", webClientConfig.getAuthorization());
        headers.set("Partner-Token", webClientConfig.getPartnerToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<VinResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, VinResponse.class);
        if (response.getStatusCode().isError()) {
            throw new RuntimeException("Error while calling external service");
        }
        VehicleInfo vehicleInfo = response.getBody().getData();
        return updateVehicleInfo(vin, vehicleInfo);
    }

    private VehicleInfo updateVehicleInfo(String vin, VehicleInfo vehicleInfo) {
        String url = String.format("http://api.carmd.com/v3.0/image?vin=%s", vin);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", webClientConfig.getAuthorization());
        headers.set("Partner-Token", webClientConfig.getPartnerToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<VinResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, VinResponse.class);
        if (response.getStatusCode().isError()) {
            throw new RuntimeException("Error while calling external service");
        }
        VehicleInfo additionalData = response.getBody().getData();
        vehicleInfo.setImage(additionalData.getImage());
        return vehicleInfo;
    }

    @GetMapping("/find_maintenance/{vin}/{mileage}")
    public List<Maintenance> getData(@PathVariable String vin, @PathVariable String mileage) {
        String url = String.format("http://api.carmd.com/v3.0/maint?vin=%s&mileage=%s", vin, mileage);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", webClientConfig.getAuthorization());
        headers.set("Partner-Token", webClientConfig.getPartnerToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<MaintenanceResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, MaintenanceResponse.class);
        if (response.getStatusCode().isError()) {
            throw new RuntimeException("Error while calling external service");
        }
        return response.getBody().getData();
    }
}