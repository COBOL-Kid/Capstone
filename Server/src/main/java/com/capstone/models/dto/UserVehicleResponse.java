package com.capstone.models.dto;

import java.util.List;

public record UserVehicleResponse(String vin, int currentMileage, Long vehicleTypeId, String make, String model,
		String trim, String year, List<String> availableImageUrls, String selectedImageUrl) {
}
