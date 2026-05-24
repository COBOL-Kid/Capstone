package com.capstone.models.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record UpdateMileageRequest(
    @PositiveOrZero(message = "Current mileage cannot be negative") int currentMileage) {}
