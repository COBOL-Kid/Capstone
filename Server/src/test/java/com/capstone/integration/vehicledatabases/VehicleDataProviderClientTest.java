package com.capstone.integration.vehicledatabases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.capstone.configuration.WebClientConfig;

class VehicleDataProviderClientTest {

    @Test
    void shouldFetchRecallsFromAutoDevEndpoint() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        WebClientConfig webClientConfig = mock(WebClientConfig.class);
        RecallResponse providerResponse = new RecallResponse(List.of());

        when(webClientConfig.getVehicleDataRecallBaseUrl()).thenReturn("https://api.auto.dev");
        when(webClientConfig.getVehicleDataRecallApiKey()).thenReturn("recall-api-key");
        when(webClientConfig.getVehicleDataRecallApiKeyHeader()).thenReturn("x-api-key");
        when(restTemplate.exchange(eq("https://api.auto.dev/openrecalls/JTENU5JR6M5962554"), eq(HttpMethod.GET),
                org.mockito.ArgumentMatchers.<HttpEntity<?>>any(), eq(RecallResponse.class)))
                .thenReturn(ResponseEntity.ok(providerResponse));

        VehicleDataProviderClient client = new VehicleDataProviderClient(restTemplate, webClientConfig);

        RecallResponse response = client.getRecalls("JTENU5JR6M5962554");

        assertSame(providerResponse, response);
        ArgumentCaptor<HttpEntity<?>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(eq("https://api.auto.dev/openrecalls/JTENU5JR6M5962554"), eq(HttpMethod.GET),
                entityCaptor.capture(), eq(RecallResponse.class));
        assertEquals("recall-api-key", entityCaptor.getValue().getHeaders().getFirst("x-api-key"));
    }
}
