package com.capstone.domain.dto;

import java.time.LocalDate;

public record CompletedMaintenanceResponse(Long completedMaintenanceId, String vin, Long maintMileageId,
		LocalDate completedDate, int mileageCompleted, Double cost, String notes) {
}
