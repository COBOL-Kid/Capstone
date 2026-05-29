package com.capstone.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class VehicleDataProviderConfig {

  @Value("${vehicle-data.auto-dev.base-url:${vehicle-data.base-url:https://api.auto.dev}}")
  private String autoDevBaseUrl;

  @Value("${vehicle-data.auto-dev.api-key:${vehicle-data.api-key:}}")
  private String autoDevApiKey;

  @Value("${vehicle-data.auto-dev.api-key-header:${vehicle-data.api-key-header:x-api-key}}")
  private String autoDevApiKeyHeader;

  @Value("${vehicle-data.vehicle-databases.base-url:https://api.vehicledatabases.com}")
  private String vehicleDatabasesBaseUrl;

  @Value("${vehicle-data.vehicle-databases.api-key:}")
  private String vehicleDatabasesApiKey;

  @Value("${vehicle-data.vehicle-databases.api-key-header:x-authkey}")
  private String vehicleDatabasesApiKeyHeader;

  @Value("${vehicle-data.http.connect-timeout-ms:5000}")
  private int connectTimeoutMs;

  @Value("${vehicle-data.http.read-timeout-ms:30000}")
  private int readTimeoutMs;

  @Bean
  public RestTemplate restTemplate() {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(connectTimeoutMs);
    requestFactory.setReadTimeout(readTimeoutMs);
    return new RestTemplate(requestFactory);
  }

  public String getAutoDevBaseUrl() {
    return autoDevBaseUrl;
  }

  public String getAutoDevApiKey() {
    return autoDevApiKey;
  }

  public String getAutoDevApiKeyHeader() {
    return autoDevApiKeyHeader;
  }

  public String getVehicleDatabasesBaseUrl() {
    return vehicleDatabasesBaseUrl;
  }

  public String getVehicleDatabasesApiKey() {
    return vehicleDatabasesApiKey;
  }

  public String getVehicleDatabasesApiKeyHeader() {
    return vehicleDatabasesApiKeyHeader;
  }
}
