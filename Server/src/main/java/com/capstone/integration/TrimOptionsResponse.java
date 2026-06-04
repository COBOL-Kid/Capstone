package com.capstone.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TrimOptionsResponse(String status, Data data) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Data(String year, String make, String model, List<String> trims) {}
}
