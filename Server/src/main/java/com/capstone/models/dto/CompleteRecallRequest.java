package com.capstone.models.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

import static com.capstone.models.dto.VinValidation.VIN_MESSAGE;
import static com.capstone.models.dto.VinValidation.VIN_PATTERN;

public record CompleteRecallRequest(
        @NotBlank(message = "VIN is required") @Pattern(regexp = VIN_PATTERN, flags = Pattern.Flag.CASE_INSENSITIVE, message = VIN_MESSAGE) String vin,
        @NotNull(message = "Recall is required") @Positive(message = "Recall must be positive") Long recallId,
        @PastOrPresent(message = "Completed date cannot be in the future") LocalDate completedDate,
        @Size(max = 255, message = "Repair shop must be 255 characters or fewer") String repairShop,
        @PositiveOrZero(message = "Cost cannot be negative") Double cost,
        @Size(max = 1000, message = "Notes must be 1000 characters or fewer") String notes) {
}
