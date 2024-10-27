package com.capstone.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WebClientConfig {

    private final String authorization = System.getenv("API_AUTH");
    private final String partnerToken = System.getenv("PARTNER_TOKEN");

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
}