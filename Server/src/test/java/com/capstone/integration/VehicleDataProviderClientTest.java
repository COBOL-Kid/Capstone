package com.capstone.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.capstone.configuration.VehicleDataProviderConfig;
import com.capstone.domain.VinNotFoundException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

class VehicleDataProviderClientTest {

  @Test
  void shouldFetchVinDecodeFromAutoDevEndpoint() {
    RestTemplate restTemplate = mock(RestTemplate.class);
    VehicleDataProviderConfig providerConfig = mock(VehicleDataProviderConfig.class);
    VinDecodeResponse providerResponse =
        new VinDecodeResponse(
            "3GCUDHEL3NG668790",
            true,
            "3GC",
            "Mexico",
            "3GCUDHELNG",
            "3",
            true,
            "Active",
            "Chevrolet",
            "Silverado 1500",
            "ZR2",
            "4x4 4dr Crew Cab 5.8 ft. SB",
            "Truck",
            "5.3L V8 OHV 16V FFV",
            "4WD",
            "Automatic",
            new VinDecodeResponse.Vehicle(
                "3GCUDHEL3NG668790",
                2022,
                "Chevrolet",
                "Silverado 1500",
                "General Motors de Mexico"),
            new VinDecodeResponse.Photos(true, false, true, 12),
            false);

    when(providerConfig.getVehicleDataBaseUrl()).thenReturn("https://api.auto.dev");
    when(providerConfig.getVehicleDataApiKey()).thenReturn("vin-api-key");
    when(providerConfig.getVehicleDataApiKeyHeader()).thenReturn("x-api-key");
    when(restTemplate.exchange(
            eq("https://api.auto.dev/vin/3GCUDHEL3NG668790"),
            eq(HttpMethod.GET),
            org.mockito.ArgumentMatchers.any(),
            eq(VinDecodeResponse.class)))
        .thenReturn(ResponseEntity.ok(providerResponse));

    VehicleDataProviderClient client = new VehicleDataProviderClient(restTemplate, providerConfig);

    VinDecodeResponse response = client.decodeVin("3GCUDHEL3NG668790");

    assertSame(providerResponse, response);
    ArgumentCaptor<HttpEntity<?>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplate)
        .exchange(
            eq("https://api.auto.dev/vin/3GCUDHEL3NG668790"),
            eq(HttpMethod.GET),
            entityCaptor.capture(),
            eq(VinDecodeResponse.class));
    assertEquals("vin-api-key", entityCaptor.getValue().getHeaders().getFirst("x-api-key"));
  }

  @Test
  void shouldFetchRecallsFromAutoDevEndpoint() {
    RestTemplate restTemplate = mock(RestTemplate.class);
    VehicleDataProviderConfig providerConfig = mock(VehicleDataProviderConfig.class);
    RecallProviderResponse providerResponse = new RecallProviderResponse(List.of());

    when(providerConfig.getVehicleDataRecallBaseUrl()).thenReturn("https://api.auto.dev");
    when(providerConfig.getVehicleDataRecallApiKey()).thenReturn("recall-api-key");
    when(providerConfig.getVehicleDataRecallApiKeyHeader()).thenReturn("x-api-key");
    when(restTemplate.exchange(
            eq("https://api.auto.dev/openrecalls/JTENU5JR6M5962554"),
            eq(HttpMethod.GET),
            org.mockito.ArgumentMatchers.any(),
            eq(RecallProviderResponse.class)))
        .thenReturn(ResponseEntity.ok(providerResponse));

    VehicleDataProviderClient client = new VehicleDataProviderClient(restTemplate, providerConfig);

    RecallProviderResponse response = client.getRecalls("JTENU5JR6M5962554");

    assertSame(providerResponse, response);
    ArgumentCaptor<HttpEntity<?>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplate)
        .exchange(
            eq("https://api.auto.dev/openrecalls/JTENU5JR6M5962554"),
            eq(HttpMethod.GET),
            entityCaptor.capture(),
            eq(RecallProviderResponse.class));
    assertEquals("recall-api-key", entityCaptor.getValue().getHeaders().getFirst("x-api-key"));
  }

  @Test
  void shouldFetchPhotosFromAutoDevEndpoint() {
    RestTemplate restTemplate = mock(RestTemplate.class);
    VehicleDataProviderConfig providerConfig = mock(VehicleDataProviderConfig.class);
    VehiclePhotosResponse providerResponse =
        new VehiclePhotosResponse(
            new VehiclePhotosResponse.PhotoData(
                List.of(
                    "https://api.auto.dev/photos/retail/JTENU5JR6M5962554-1.jpg",
                    "https://api.auto.dev/photos/retail/JTENU5JR6M5962554-2.jpg")));

    when(providerConfig.getVehicleDataBaseUrl()).thenReturn("https://api.auto.dev");
    when(providerConfig.getVehicleDataApiKey()).thenReturn("vin-api-key");
    when(providerConfig.getVehicleDataApiKeyHeader()).thenReturn("x-api-key");
    when(restTemplate.exchange(
            eq("https://api.auto.dev/photos/JTENU5JR6M5962554"),
            eq(HttpMethod.GET),
            org.mockito.ArgumentMatchers.any(),
            eq(VehiclePhotosResponse.class)))
        .thenReturn(ResponseEntity.ok(providerResponse));

    VehicleDataProviderClient client = new VehicleDataProviderClient(restTemplate, providerConfig);

    VehiclePhotosResponse response = client.getPhotos("JTENU5JR6M5962554");

    assertSame(providerResponse, response);
    ArgumentCaptor<HttpEntity<?>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplate)
        .exchange(
            eq("https://api.auto.dev/photos/JTENU5JR6M5962554"),
            eq(HttpMethod.GET),
            entityCaptor.capture(),
            eq(VehiclePhotosResponse.class));
    assertEquals("vin-api-key", entityCaptor.getValue().getHeaders().getFirst("x-api-key"));
    assertEquals(2, response.retailPhotos().size());
  }

  @Test
  void shouldThrowVinNotFoundWhenVinDecodeReturns404() {
    RestTemplate restTemplate = mock(RestTemplate.class);
    VehicleDataProviderConfig providerConfig = mock(VehicleDataProviderConfig.class);

    when(providerConfig.getVehicleDataBaseUrl()).thenReturn("https://api.auto.dev");
    when(providerConfig.getVehicleDataApiKey()).thenReturn("vin-api-key");
    when(providerConfig.getVehicleDataApiKeyHeader()).thenReturn("x-api-key");
    when(restTemplate.exchange(
            eq("https://api.auto.dev/vin/MISSINGVIN1234567"),
            eq(HttpMethod.GET),
            org.mockito.ArgumentMatchers.any(),
            eq(VinDecodeResponse.class)))
        .thenThrow(
            HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null));

    VehicleDataProviderClient client = new VehicleDataProviderClient(restTemplate, providerConfig);

    assertThrows(VinNotFoundException.class, () -> client.decodeVin("MISSINGVIN1234567"));
  }

  @Test
  void shouldReturnNullWhenSupplementalEndpointReturns404() {
    RestTemplate restTemplate = mock(RestTemplate.class);
    VehicleDataProviderConfig providerConfig = mock(VehicleDataProviderConfig.class);

    when(providerConfig.getVehicleDataBaseUrl()).thenReturn("https://api.auto.dev");
    when(providerConfig.getVehicleDataApiKey()).thenReturn("vin-api-key");
    when(providerConfig.getVehicleDataApiKeyHeader()).thenReturn("x-api-key");
    when(restTemplate.exchange(
            eq("https://api.auto.dev/photos/JTENU5JR6M5962554"),
            eq(HttpMethod.GET),
            org.mockito.ArgumentMatchers.any(),
            eq(VehiclePhotosResponse.class)))
        .thenThrow(
            HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null));

    VehicleDataProviderClient client = new VehicleDataProviderClient(restTemplate, providerConfig);

    assertNull(client.getPhotos("JTENU5JR6M5962554"));
  }
}
