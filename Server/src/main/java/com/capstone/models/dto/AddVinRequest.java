package com.capstone.models.dto;

import static com.capstone.models.dto.VinValidation.VIN_MESSAGE;
import static com.capstone.models.dto.VinValidation.VIN_PATTERN;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

public record AddVinRequest(
    @NotBlank(message = "VIN is required")
        @Pattern(regexp = VIN_PATTERN, flags = Pattern.Flag.CASE_INSENSITIVE, message = VIN_MESSAGE)
        String vin,
    @NotNull(message = "Current mileage is required")
        @PositiveOrZero(message = "Current mileage cannot be negative")
        Integer currentMileage,
    String selectedTrim) {

  public boolean hasSelectedTrim() {
    return selectedTrim != null && !selectedTrim.isBlank();
  }
}
