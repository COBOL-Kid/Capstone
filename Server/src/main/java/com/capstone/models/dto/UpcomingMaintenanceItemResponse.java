package com.capstone.models.dto;

import java.util.List;

public record UpcomingMaintenanceItemResponse(
    Long maintMileageId,
    String maintDesc,
    boolean isInspect,
    LaborCostResponse labor,
    List<PartCostResponse> parts) {}
