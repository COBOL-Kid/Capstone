package com.capstone.models.dto;

import java.math.BigDecimal;

public record LaborCostResponse(
    BigDecimal timeRequiredHours, BigDecimal hourlyRate, BigDecimal totalCost, String currency) {}
