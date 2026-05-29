package com.capstone.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RepairEstimatesResponse(String status, RepairEstimatesData data) {

  public record RepairEstimatesData(
      String vin,
      Integer year,
      String make,
      String model,
      String trim,
      List<MileageInterval> data) {}

  public record MileageInterval(String mileage, List<EstimateItem> items) {}

  public record EstimateItem(List<PartLine> parts, List<LaborLine> labor, List<TotalLine> total) {}

  public record PartLine(
      String type, @JsonProperty("total_cost") BigDecimal totalCost, String currency) {}

  public record LaborLine(
      String type,
      @JsonProperty("time_required_hours") BigDecimal timeRequiredHours,
      @JsonProperty("hourly_rate") BigDecimal hourlyRate,
      @JsonProperty("total_cost") BigDecimal totalCost,
      String currency) {}

  public record TotalLine(
      String type, @JsonProperty("total_cost") BigDecimal totalCost, String currency) {}
}
