package com.capstone.models.dto;

import java.util.List;

public record UpcomingMaintenanceIntervalResponse(
    int mileageDue,
    MileageCostSummaryResponse summary,
    List<UpcomingMaintenanceItemResponse> items) {}
