package com.capstone.models.dto;

import java.time.LocalDate;

public record WarrantyCoverageResponse(
    String coverageName,
    String coverageValue,
    LocalDate estimatedExpirationDate,
    boolean expired,
    Integer remainingMonths,
    Integer remainingMiles) {}
