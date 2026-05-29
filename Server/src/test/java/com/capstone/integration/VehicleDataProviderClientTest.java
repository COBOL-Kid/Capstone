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

  private static final String AUTO_DEV_BASE = "https://api.auto.dev";
  private static final String VDB_BASE = "https://api.vehicledatabases.com";

  @Test
  void shouldFetchVinDecodeFromAutoDevEndpoint() {
    RestTemplate restTemplate = mock(RestTemplate.class);
    VehicleDataProviderConfig providerConfig = mock(VehicleDataProviderConfig.class);
    VinDecodeResponse providerResponse = sampleVinDecode();

    when(providerConfig.getAutoDevBaseUrl()).thenReturn(AUTO_DEV_BASE);
    when(providerConfig.getAutoDevApiKey()).thenReturn("auto-dev-key");
    when(providerConfig.getAutoDevApiKeyHeader()).thenReturn("x-api-key");
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
    assertEquals("auto-dev-key", entityCaptor.getValue().getHeaders().getFirst("x-api-key"));
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

    when(providerConfig.getAutoDevBaseUrl()).thenReturn(AUTO_DEV_BASE);
    when(providerConfig.getAutoDevApiKey()).thenReturn("auto-dev-key");
    when(providerConfig.getAutoDevApiKeyHeader()).thenReturn("x-api-key");
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
    assertEquals("auto-dev-key", entityCaptor.getValue().getHeaders().getFirst("x-api-key"));
    assertEquals(2, response.retailPhotos().size());
  }

  @Test
  void shouldFetchRecallsFromVehicleDatabasesEndpoint() {
    RestTemplate restTemplate = mock(RestTemplate.class);
    VehicleDataProviderConfig providerConfig = mock(VehicleDataProviderConfig.class);
    VehicleRecallsResponse providerResponse =
        new VehicleRecallsResponse(
            "success",
            new VehicleRecallsResponse.VehicleRecallsData(
                "5J6YH28728L014142", "2008", "Honda", "Element", List.of()));

    stubVehicleDatabases(
        providerConfig,
        restTemplate,
        "https://api.vehicledatabases.com/vehicle-recalls/JTENU5JR6M5962554",
        VehicleRecallsResponse.class,
        providerResponse);

    VehicleDataProviderClient client = new VehicleDataProviderClient(restTemplate, providerConfig);

    VehicleRecallsResponse response = client.getRecalls("JTENU5JR6M5962554");

    assertEquals("success", response.status());
    verifyVehicleDatabasesAuth(
        restTemplate,
        "https://api.vehicledatabases.com/vehicle-recalls/JTENU5JR6M5962554",
        VehicleRecallsResponse.class);
  }

  @Test
  void shouldFetchRepairEstimatesFromVehicleDatabasesEndpoint() {
    RestTemplate restTemplate = mock(RestTemplate.class);
    VehicleDataProviderConfig providerConfig = mock(VehicleDataProviderConfig.class);
    RepairEstimatesResponse providerResponse = new RepairEstimatesResponse("success", null);

    stubVehicleDatabases(
        providerConfig,
        restTemplate,
        "https://api.vehicledatabases.com/repair-estimates/JTENU5JR6M5962554",
        RepairEstimatesResponse.class,
        providerResponse);

    VehicleDataProviderClient client = new VehicleDataProviderClient(restTemplate, providerConfig);

    RepairEstimatesResponse response = client.getRepairEstimates("JTENU5JR6M5962554");

    assertEquals("success", response.status());
    verifyVehicleDatabasesAuth(
        restTemplate,
        "https://api.vehicledatabases.com/repair-estimates/JTENU5JR6M5962554",
        RepairEstimatesResponse.class);
  }

  @Test
  void shouldFetchRepairCostsFromVehicleDatabasesEndpoint() {
    RestTemplate restTemplate = mock(RestTemplate.class);
    VehicleDataProviderConfig providerConfig = mock(VehicleDataProviderConfig.class);
    RepairCostResponse providerResponse = new RepairCostResponse("success", null);

    stubVehicleDatabases(
        providerConfig,
        restTemplate,
        "https://api.vehicledatabases.com/vehicle-repairs/v2/JTENU5JR6M5962554",
        RepairCostResponse.class,
        providerResponse);

    VehicleDataProviderClient client = new VehicleDataProviderClient(restTemplate, providerConfig);

    RepairCostResponse response = client.getRepairCosts("JTENU5JR6M5962554");

    assertEquals("success", response.status());
    verifyVehicleDatabasesAuth(
        restTemplate,
        "https://api.vehicledatabases.com/vehicle-repairs/v2/JTENU5JR6M5962554",
        RepairCostResponse.class);
  }

  @Test
  void shouldFetchOwnerManualFromVehicleDatabasesEndpoint() {
    RestTemplate restTemplate = mock(RestTemplate.class);
    VehicleDataProviderConfig providerConfig = mock(VehicleDataProviderConfig.class);
    OwnerManualResponse providerResponse =
        new OwnerManualResponse(
            "success",
            "5J8YD4H83LL002807",
            new OwnerManualResponse.OwnerManualData(
                null, "2020", "Acura", "MDX", "https://example.com/manual.pdf"));

    stubVehicleDatabases(
        providerConfig,
        restTemplate,
        "https://api.vehicledatabases.com/owner-manual/JTENU5JR6M5962554",
        OwnerManualResponse.class,
        providerResponse);

    VehicleDataProviderClient client = new VehicleDataProviderClient(restTemplate, providerConfig);

    OwnerManualResponse response = client.getOwnerManual("JTENU5JR6M5962554");

    assertEquals("success", response.status());
    verifyVehicleDatabasesAuth(
        restTemplate,
        "https://api.vehicledatabases.com/owner-manual/JTENU5JR6M5962554",
        OwnerManualResponse.class);
  }

  @Test
  void shouldThrowWhenProviderReturnsEmptyBody() {
    RestTemplate restTemplate = mock(RestTemplate.class);
    VehicleDataProviderConfig providerConfig = mock(VehicleDataProviderConfig.class);

    when(providerConfig.getAutoDevBaseUrl()).thenReturn(AUTO_DEV_BASE);
    when(providerConfig.getAutoDevApiKey()).thenReturn("auto-dev-key");
    when(providerConfig.getAutoDevApiKeyHeader()).thenReturn("x-api-key");
    when(restTemplate.exchange(
            eq("https://api.auto.dev/vin/3GCUDHEL3NG668790"),
            eq(HttpMethod.GET),
            org.mockito.ArgumentMatchers.any(),
            eq(VinDecodeResponse.class)))
        .thenReturn(ResponseEntity.ok(null));

    VehicleDataProviderClient client = new VehicleDataProviderClient(restTemplate, providerConfig);

    assertThrows(IllegalStateException.class, () -> client.decodeVin("3GCUDHEL3NG668790"));
  }

  @Test
  void shouldRethrowNonNotFoundHttpErrors() {
    RestTemplate restTemplate = mock(RestTemplate.class);
    VehicleDataProviderConfig providerConfig = mock(VehicleDataProviderConfig.class);

    when(providerConfig.getVehicleDatabasesBaseUrl()).thenReturn(VDB_BASE);
    when(providerConfig.getVehicleDatabasesApiKey()).thenReturn("vdb-key");
    when(providerConfig.getVehicleDatabasesApiKeyHeader()).thenReturn("x-authkey");
    when(restTemplate.exchange(
            eq("https://api.vehicledatabases.com/repair-estimates/JTENU5JR6M5962554"),
            eq(HttpMethod.GET),
            org.mockito.ArgumentMatchers.any(),
            eq(RepairEstimatesResponse.class)))
        .thenThrow(
            HttpClientErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR, "Server error", null, null, null));

    VehicleDataProviderClient client = new VehicleDataProviderClient(restTemplate, providerConfig);

    HttpClientErrorException ex =
        assertThrows(
            HttpClientErrorException.class, () -> client.getRepairEstimates("JTENU5JR6M5962554"));
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
  }

  @Test
  void shouldReturnNullWhenRecallsEndpointReturns404() {
    RestTemplate restTemplate = mock(RestTemplate.class);
    VehicleDataProviderConfig providerConfig = mock(VehicleDataProviderConfig.class);

    when(providerConfig.getVehicleDatabasesBaseUrl()).thenReturn(VDB_BASE);
    when(providerConfig.getVehicleDatabasesApiKey()).thenReturn("vdb-key");
    when(providerConfig.getVehicleDatabasesApiKeyHeader()).thenReturn("x-authkey");
    when(restTemplate.exchange(
            eq("https://api.vehicledatabases.com/vehicle-recalls/JTENU5JR6M5962554"),
            eq(HttpMethod.GET),
            org.mockito.ArgumentMatchers.any(),
            eq(VehicleRecallsResponse.class)))
        .thenThrow(
            HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null));

    VehicleDataProviderClient client = new VehicleDataProviderClient(restTemplate, providerConfig);

    assertNull(client.getRecalls("JTENU5JR6M5962554"));
  }

  @Test
  void shouldThrowVinNotFoundWhenVinDecodeReturns404() {
    RestTemplate restTemplate = mock(RestTemplate.class);
    VehicleDataProviderConfig providerConfig = mock(VehicleDataProviderConfig.class);

    when(providerConfig.getAutoDevBaseUrl()).thenReturn(AUTO_DEV_BASE);
    when(providerConfig.getAutoDevApiKey()).thenReturn("auto-dev-key");
    when(providerConfig.getAutoDevApiKeyHeader()).thenReturn("x-api-key");
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
  void shouldReturnNullWhenVehicleDatabasesEndpointReturns404() {
    RestTemplate restTemplate = mock(RestTemplate.class);
    VehicleDataProviderConfig providerConfig = mock(VehicleDataProviderConfig.class);

    when(providerConfig.getVehicleDatabasesBaseUrl()).thenReturn(VDB_BASE);
    when(providerConfig.getVehicleDatabasesApiKey()).thenReturn("vdb-key");
    when(providerConfig.getVehicleDatabasesApiKeyHeader()).thenReturn("x-authkey");
    when(restTemplate.exchange(
            eq("https://api.vehicledatabases.com/repair-estimates/JTENU5JR6M5962554"),
            eq(HttpMethod.GET),
            org.mockito.ArgumentMatchers.any(),
            eq(RepairEstimatesResponse.class)))
        .thenThrow(
            HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null));

    VehicleDataProviderClient client = new VehicleDataProviderClient(restTemplate, providerConfig);

    assertNull(client.getRepairEstimates("JTENU5JR6M5962554"));
  }

  private static <T> void stubVehicleDatabases(
      VehicleDataProviderConfig providerConfig,
      RestTemplate restTemplate,
      String url,
      Class<T> responseType,
      T providerResponse) {
    when(providerConfig.getVehicleDatabasesBaseUrl()).thenReturn(VDB_BASE);
    when(providerConfig.getVehicleDatabasesApiKey()).thenReturn("vdb-key");
    when(providerConfig.getVehicleDatabasesApiKeyHeader()).thenReturn("x-authkey");
    when(restTemplate.exchange(
            eq(url), eq(HttpMethod.GET), org.mockito.ArgumentMatchers.any(), eq(responseType)))
        .thenReturn(ResponseEntity.ok(providerResponse));
  }

  private static <T> void verifyVehicleDatabasesAuth(
      RestTemplate restTemplate, String url, Class<T> responseType) {
    ArgumentCaptor<HttpEntity<?>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplate)
        .exchange(eq(url), eq(HttpMethod.GET), entityCaptor.capture(), eq(responseType));
    assertEquals("vdb-key", entityCaptor.getValue().getHeaders().getFirst("x-authkey"));
  }

  private static VinDecodeResponse sampleVinDecode() {
    return new VinDecodeResponse(
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
            "3GCUDHEL3NG668790", 2022, "Chevrolet", "Silverado 1500", "General Motors de Mexico"),
        new VinDecodeResponse.Photos(true, false, true, 12),
        false);
  }
}
