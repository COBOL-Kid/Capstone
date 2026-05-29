package com.capstone.models.dto;

public record MiscMaintenanceCostResponse(
    Long miscMaintCostId,
    String maintTitle,
    String maintDesc,
    Integer independentAvg,
    Integer independentHigh,
    Integer independentLow,
    Integer dealerAvg,
    Integer dealerHigh,
    Integer dealerLow) {}
