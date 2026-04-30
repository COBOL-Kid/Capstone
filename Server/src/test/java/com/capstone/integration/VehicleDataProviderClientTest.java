package com.capstone.integration;

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
	void shouldFetchVinDecodeFromAutoDevEndpoint() {
		RestTemplate restTemplate = mock(RestTemplate.class);
		WebClientConfig webClientConfig = mock(WebClientConfig.class);
		VinDecodeResponse providerResponse = new VinDecodeResponse("3GCUDHEL3NG668790", true, "3GC", "Mexico",
				"3GCUDHELNG", "3", true, "Active", "Chevrolet", "Silverado 1500", "ZR2", "4x4 4dr Crew Cab 5.8 ft. SB",
				"Truck", "5.3L V8 OHV 16V FFV", "4WD", "Automatic", new VinDecodeResponse.Vehicle("3GCUDHEL3NG668790",
						2022, "Chevrolet", "Silverado 1500", "General Motors de Mexico"),
				new VinDecodeResponse.Photos(true, false, true, 12), false);

		when(webClientConfig.getVehicleDataBaseUrl()).thenReturn("https://api.auto.dev");
		when(webClientConfig.getVehicleDataApiKey()).thenReturn("vin-api-key");
		when(webClientConfig.getVehicleDataApiKeyHeader()).thenReturn("x-api-key");
		when(restTemplate.exchange(eq("https://api.auto.dev/vin/3GCUDHEL3NG668790"), eq(HttpMethod.GET),
				org.mockito.ArgumentMatchers.<HttpEntity<?>>any(), eq(VinDecodeResponse.class)))
				.thenReturn(ResponseEntity.ok(providerResponse));

		VehicleDataProviderClient client = new VehicleDataProviderClient(restTemplate, webClientConfig);

		VinDecodeResponse response = client.decodeVin("3GCUDHEL3NG668790");

		assertSame(providerResponse, response);
		ArgumentCaptor<HttpEntity<?>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
		verify(restTemplate).exchange(eq("https://api.auto.dev/vin/3GCUDHEL3NG668790"), eq(HttpMethod.GET),
				entityCaptor.capture(), eq(VinDecodeResponse.class));
		assertEquals("vin-api-key", entityCaptor.getValue().getHeaders().getFirst("x-api-key"));
	}

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

	@Test
	void shouldFetchPhotosFromAutoDevEndpoint() {
		RestTemplate restTemplate = mock(RestTemplate.class);
		WebClientConfig webClientConfig = mock(WebClientConfig.class);
		VehiclePhotosResponse providerResponse = new VehiclePhotosResponse(new VehiclePhotosResponse.PhotoData(
				List.of("https://api.auto.dev/photos/retail/JTENU5JR6M5962554-1.jpg",
						"https://api.auto.dev/photos/retail/JTENU5JR6M5962554-2.jpg")));

		when(webClientConfig.getVehicleDataBaseUrl()).thenReturn("https://api.auto.dev");
		when(webClientConfig.getVehicleDataApiKey()).thenReturn("vin-api-key");
		when(webClientConfig.getVehicleDataApiKeyHeader()).thenReturn("x-api-key");
		when(restTemplate.exchange(eq("https://api.auto.dev/photos/JTENU5JR6M5962554"), eq(HttpMethod.GET),
				org.mockito.ArgumentMatchers.<HttpEntity<?>>any(), eq(VehiclePhotosResponse.class)))
				.thenReturn(ResponseEntity.ok(providerResponse));

		VehicleDataProviderClient client = new VehicleDataProviderClient(restTemplate, webClientConfig);

		VehiclePhotosResponse response = client.getPhotos("JTENU5JR6M5962554");

		assertSame(providerResponse, response);
		ArgumentCaptor<HttpEntity<?>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
		verify(restTemplate).exchange(eq("https://api.auto.dev/photos/JTENU5JR6M5962554"), eq(HttpMethod.GET),
				entityCaptor.capture(), eq(VehiclePhotosResponse.class));
		assertEquals("vin-api-key", entityCaptor.getValue().getHeaders().getFirst("x-api-key"));
		assertEquals(2, response.retailPhotos().size());
	}
}
