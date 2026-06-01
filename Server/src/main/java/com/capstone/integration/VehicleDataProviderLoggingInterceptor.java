package com.capstone.integration;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

@Component
public class VehicleDataProviderLoggingInterceptor implements ClientHttpRequestInterceptor {

  private static final Logger log =
      LoggerFactory.getLogger(VehicleDataProviderLoggingInterceptor.class);

  private final VehicleDataProviderRequestMetrics metrics;
  private final boolean logRequests;

  public VehicleDataProviderLoggingInterceptor(
      VehicleDataProviderRequestMetrics metrics,
      @Value("${vehicle-data.http.log-requests:false}") boolean logRequests) {
    this.metrics = metrics;
    this.logRequests = logRequests;
  }

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    metrics.requestStarted();
    if (logRequests) {
      log.debug("HTTP {} {}", request.getMethod(), request.getURI());
    }
    try {
      return execution.execute(request, body);
    } finally {
      metrics.requestFinished();
    }
  }
}
