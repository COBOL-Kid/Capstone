package com.capstone.integration;

import java.util.List;

public record VehiclePhotosResponse(PhotoData data) {

	public List<String> retailPhotos() {
		if (data == null || data.retail() == null) {
			return List.of();
		}
		return data.retail();
	}

	public record PhotoData(List<String> retail) {
	}
}
