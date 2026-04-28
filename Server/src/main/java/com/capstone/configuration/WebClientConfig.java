package com.capstone.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WebClientConfig {

    @Value("${API_AUTH:}")
    private String authorization;

    @Value("${API_TOKEN:}")
    private String partnerToken;

    @Value("${vehicle-data.base-url:https://api.vehicledatabases.com}")
    private String vehicleDataBaseUrl;

    @Value("${vehicle-data.api-key:}")
    private String vehicleDataApiKey;

    @Value("${vehicle-data.api-key-header:x-AuthKey}")
    private String vehicleDataApiKeyHeader;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public String getAuthorization() {
        return authorization;
    }

    public String getPartnerToken() {
        return partnerToken;
    }

    public String getVehicleDataBaseUrl() {
        return vehicleDataBaseUrl;
    }

    public String getVehicleDataApiKey() {
        return vehicleDataApiKey;
    }

    public String getVehicleDataApiKeyHeader() {
        return vehicleDataApiKeyHeader;
    }
}