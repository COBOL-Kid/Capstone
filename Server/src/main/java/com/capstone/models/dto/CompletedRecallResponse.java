package com.capstone.models.dto;

import java.time.LocalDate;

public record CompletedRecallResponse(Long completedRecallId, String vin, Long recallId, LocalDate completedDate,
		String repairShop, Double cost, String notes) {
}
