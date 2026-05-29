package com.capstone.models.dto;

import java.util.List;

public record VehicleDashboardResponse(
    VehicleDetailResponse detail,
    List<UpcomingMaintenanceIntervalResponse> upcomingMaintenance,
    List<CompletedMaintenanceResponse> completedMaintenance,
    List<RecallResponse> uncompletedRecalls,
    List<CompletedRecallResponse> completedRecalls,
    List<MiscMaintenanceCostResponse> miscMaintenanceCosts) {}
