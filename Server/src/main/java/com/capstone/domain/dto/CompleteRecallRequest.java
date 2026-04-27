package com.capstone.domain.dto;

import java.time.LocalDate;

public record CompleteRecallRequest(String vin, Long recallId, LocalDate completedDate, String repairShop, Double cost,
        String notes) {
}