package com.capstone.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RepairCostResponse(String status, RepairCostData data) {

  public record RepairCostData(
      String vin,
      Integer year,
      String make,
      String model,
      String currency,
      List<RepairItem> repair) {}

  public record RepairItem(String title, String description, Costs costs) {}

  public record Costs(List<CostLine> independent, List<CostLine> dealer) {}

  public record CostLine(String name, Integer average, Integer high, Integer low) {}
}
