package com.capstone.integration;

public record OwnerManualResponse(String status, OwnerManualData data) {

	public record OwnerManualData(String vin, String year, String make, String model, String path) {
	}
}
