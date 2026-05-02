package com.capstone.models.dto;

public final class VinValidation {

	public static final String VIN_PATTERN = "^\\s*[A-HJ-NPR-Z0-9]{17}\\s*$";
	public static final String VIN_MESSAGE = "VIN must be 17 characters and cannot contain I, O, or Q";

	private VinValidation() {
	}
}
