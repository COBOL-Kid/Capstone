package com.capstone.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VehiclePhotosResponse(PhotoData data) {

  public List<String> retailPhotos() {
    if (data == null || data.retail() == null) {
      return List.of();
    }
    return data.retail();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record PhotoData(List<String> retail) {}
}
