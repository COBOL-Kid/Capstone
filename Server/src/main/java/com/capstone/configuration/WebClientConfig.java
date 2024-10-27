package com.capstone.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WebClientConfig {

    @Value("${api.auth}")
    private String authorization;

    @Value("${partner.token}")
    private String partnerToken;

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