package com.capstone.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VehicleWarrantyResponse(String status, WarrantyData data) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record WarrantyData(
      String year, String make, String model, Map<String, String> warranty) {}
}
