package com.capstone.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    private final String authorization = System.getenv("API_AUTH");

    private final String partnerToken = System.getenv("PARTNER_TOKEN");

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl("http://api.carmd.com/v3.0")
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Authorization", authorization)
                .defaultHeader("Partner-Token", partnerToken)
                .build();
    }
}