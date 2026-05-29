package com.capstone.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OwnerManualResponse(String status, String vin, OwnerManualData data) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record OwnerManualData(String vin, String year, String make, String model, String path) {}
}
