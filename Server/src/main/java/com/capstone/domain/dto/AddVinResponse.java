package com.capstone.domain.dto;

import java.util.List;

public record AddVinResponse(String vin, int currentMileage, Long vehicleTypeId, String make, String model, String trim,
		String year, List<String> availableImageUrls, String selectedImageUrl, boolean createdVin,
		boolean createdVehicleType, boolean createdAssociation) {
}
