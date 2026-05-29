package com.capstone.models.dto;

import java.math.BigDecimal;

public record MileageCostSummaryResponse(
    BigDecimal totalPartsCost, BigDecimal totalLaborCost, BigDecimal totalCost, String currency) {}
