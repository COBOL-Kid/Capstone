package com.capstone.models.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateVehiclePhotoRequest(@NotBlank(message = "Selected image URL is required") String selectedImageUrl) {
}
