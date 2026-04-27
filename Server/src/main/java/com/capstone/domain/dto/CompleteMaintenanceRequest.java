package com.capstone.domain.dto;

import static com.capstone.domain.dto.VinValidation.VIN_MESSAGE;
import static com.capstone.domain.dto.VinValidation.VIN_PATTERN;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CompleteMaintenanceRequest(
        @NotBlank(message = "VIN is required")
        @Pattern(regexp = VIN_PATTERN, flags = Pattern.Flag.CASE_INSENSITIVE, message = VIN_MESSAGE)
        String vin,
        @NotNull(message = "Maintenance item is required")
        @Positive(message = "Maintenance item must be positive")
        Long maintMileageId,
        @PastOrPresent(message = "Completed date cannot be in the future") LocalDate completedDate,
        @NotNull(message = "Mileage completed is required")
        @PositiveOrZero(message = "Mileage completed cannot be negative")
        Integer mileageCompleted,
        @PositiveOrZero(message = "Cost cannot be negative") Double cost,
        @Size(max = 1000, message = "Notes must be 1000 characters or fewer") String notes) {
}