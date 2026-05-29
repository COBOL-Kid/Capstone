package com.capstone.models.dto;

import java.math.BigDecimal;

public record PartCostResponse(String partDesc, BigDecimal totalCost, String currency) {}
