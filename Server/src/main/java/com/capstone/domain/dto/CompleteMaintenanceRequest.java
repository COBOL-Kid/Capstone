package com.capstone.domain.dto;

import java.time.LocalDate;

public record CompleteMaintenanceRequest(String vin, Long maintMileageId, LocalDate completedDate,
        int mileageCompleted, Double cost, String notes) {
}