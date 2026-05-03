package com.capstone.models.dto;

public record VinPublicResponse(String vin, Long vehicleTypeId, String vehicleMake, String vehicleModel,
                                String vehicleTrim, String vehicleYear, String vehicleStyle) {
}
