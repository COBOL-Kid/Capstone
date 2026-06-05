package com.capstone.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

class VehicleDataProviderRequestMetricsInterceptorTest {

  @Test
  void formatTargetUsesHostAndPathWithoutQuery() {
    assertEquals(
        "api.auto.dev/vin/1234",
        VehicleDataProviderRequestMetricsInterceptor.formatTarget(
            URI.create("https://api.auto.dev/vin/1234?apiKey=secret")));
  }

  @Test
  void recordsMetricsOnSuccess() throws IOException {
    VehicleDataProviderRequestMetrics metrics = new VehicleDataProviderRequestMetrics();
    VehicleDataProviderRequestMetricsInterceptor interceptor =
        new VehicleDataProviderRequestMetricsInterceptor(metrics, 60_000);
    HttpRequest request =
        new MockClientHttpRequest(HttpMethod.GET, URI.create("https://example.com/a"));
    ClientHttpResponse response = mock(ClientHttpResponse.class);
    when(response.getStatusCode()).thenReturn(HttpStatus.OK);
    ClientHttpRequestExecution execution = (req, body) -> response;

    ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);

    assertSame(response, result);
    assertEquals(0, metrics.globalInFlight());
  }

  @Test
  void recordsMetricsWhenExecutionFails() {
    VehicleDataProviderRequestMetrics metrics = new VehicleDataProviderRequestMetrics();
    VehicleDataProviderRequestMetricsInterceptor interceptor =
        new VehicleDataProviderRequestMetricsInterceptor(metrics, 60_000);
    HttpRequest request =
        new MockClientHttpRequest(HttpMethod.GET, URI.create("https://example.com/a"));
    ResourceAccessException failure = new ResourceAccessException("Connection timed out");
    ClientHttpRequestExecution execution =
        (req, body) -> {
          throw failure;
        };

    ResourceAccessException thrown =
        assertThrows(
            ResourceAccessException.class,
            () -> interceptor.intercept(request, new byte[0], execution));

    assertSame(failure, thrown);
    assertEquals(0, metrics.globalInFlight());
  }

  @Test
  void propagatesHttpStatusCodeException() {
    VehicleDataProviderRequestMetrics metrics = new VehicleDataProviderRequestMetrics();
    VehicleDataProviderRequestMetricsInterceptor interceptor =
        new VehicleDataProviderRequestMetricsInterceptor(metrics, 60_000);
    HttpRequest request =
        new MockClientHttpRequest(HttpMethod.GET, URI.create("https://example.com/a"));
    HttpClientErrorException failure =
        HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null);
    ClientHttpRequestExecution execution =
        (req, body) -> {
          throw failure;
        };

    HttpClientErrorException thrown =
        assertThrows(
            HttpClientErrorException.class,
            () -> interceptor.intercept(request, new byte[0], execution));

    assertSame(failure, thrown);
    assertEquals(0, metrics.globalInFlight());
  }
}
