package com.capstone.models.dto;

public record UpcomingMaintenanceResponse(
        Long maintMileageId,
        String vin,
        int mileageDue,
        String maintDesc
) {
}
