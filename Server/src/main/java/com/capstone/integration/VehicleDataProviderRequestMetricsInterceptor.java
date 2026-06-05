package com.capstone.integration;

import java.io.IOException;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

@Component
public class VehicleDataProviderRequestMetricsInterceptor implements ClientHttpRequestInterceptor {

  private static final Logger log =
      LoggerFactory.getLogger(VehicleDataProviderRequestMetricsInterceptor.class);

  private final VehicleDataProviderRequestMetrics metrics;
  private final long slowRequestThresholdMs;

  public VehicleDataProviderRequestMetricsInterceptor(
      VehicleDataProviderRequestMetrics metrics,
      @Value("${vehicle-data.http.slow-request-threshold-ms:10000}") long slowRequestThresholdMs) {
    this.metrics = metrics;
    this.slowRequestThresholdMs = slowRequestThresholdMs;
  }

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    String method = request.getMethod().name();
    String target = formatTarget(request.getURI());
    metrics.requestStarted();
    long startNanos = System.nanoTime();
    try {
      ClientHttpResponse response = execution.execute(request, body);
      logIfSlow(method, target, durationMs(startNanos), response.getStatusCode().value());
      return response;
    } catch (HttpStatusCodeException ex) {
      logIfSlow(method, target, durationMs(startNanos), ex.getStatusCode().value());
      throw ex;
    } catch (RestClientException ex) {
      log.warn(
          "Vehicle data HTTP transport error method={} target={} durationMs={} error={}",
          method,
          target,
          durationMs(startNanos),
          ex.getMessage());
      throw ex;
    } catch (IOException ex) {
      log.warn(
          "Vehicle data HTTP I/O error method={} target={} durationMs={} error={}",
          method,
          target,
          durationMs(startNanos),
          ex.getMessage());
      throw ex;
    } finally {
      metrics.requestFinished();
    }
  }

  private void logIfSlow(String method, String target, long durationMs, int status) {
    if (durationMs >= slowRequestThresholdMs) {
      log.warn(
          "Vehicle data HTTP slow method={} target={} durationMs={} status={}",
          method,
          target,
          durationMs,
          status);
    }
  }

  private static long durationMs(long startNanos) {
    return (System.nanoTime() - startNanos) / 1_000_000L;
  }

  static String formatTarget(URI uri) {
    if (uri == null) {
      return "unknown";
    }
    String host = uri.getHost();
    if (host == null || host.isBlank()) {
      return "unknown";
    }
    String path = uri.getRawPath();
    if (path == null || path.isBlank()) {
      return host;
    }
    return host + path;
  }
}
