package com.capstone.configuration;

import com.capstone.integration.VehicleDataProviderLoggingInterceptor;
import com.capstone.integration.VehicleDatabasesRateLimiter;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class VehicleDataProviderConfig {

  @Value("${vehicle-data.http.connect-timeout-ms:5000}")
  private int connectTimeoutMs;

  @Value("${vehicle-data.http.read-timeout-ms:30000}")
  private int readTimeoutMs;

  @Bean
  public VehicleDatabasesRateLimiter vehicleDatabasesRateLimiter(
      @Value("${vehicle-data.vehicle-databases.max-requests-per-second:2}")
          double maxRequestsPerSecond) {
    return new VehicleDatabasesRateLimiter(maxRequestsPerSecond);
  }

  @Bean
  public RestTemplate restTemplate(VehicleDataProviderLoggingInterceptor loggingInterceptor) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(connectTimeoutMs);
    requestFactory.setReadTimeout(readTimeoutMs);
    RestTemplate restTemplate =
        new RestTemplate(new BufferingClientHttpRequestFactory(requestFactory));
    restTemplate.setInterceptors(List.of(loggingInterceptor));
    return restTemplate;
  }
}
