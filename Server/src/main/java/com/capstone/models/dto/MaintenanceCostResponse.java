package com.capstone.models.dto;

public record MaintenanceCostResponse(
    Long maintCostId,
    String maintTitle,
    String maintDesc,
    Integer independentAvg,
    Integer independentHigh,
    Integer independentLow,
    Integer dealerAvg,
    Integer dealerHigh,
    Integer dealerLow) {}
