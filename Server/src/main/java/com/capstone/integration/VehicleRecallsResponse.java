package com.capstone.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VehicleRecallsResponse(String status, VehicleRecallsData data) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record VehicleRecallsData(
      String vin, String year, String make, String model, List<RecallEntry> recall) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record RecallEntry(
      @JsonProperty("campaign_id") String campaignId,
      @JsonProperty("recall_no") String recallNo,
      @JsonProperty("recall_date") String recallDate,
      @JsonProperty("component_affected") String componentAffected,
      String summary,
      String consequences,
      String remedy,
      String notes,
      @JsonProperty("manufacturer_name") String manufacturerName) {}
}
