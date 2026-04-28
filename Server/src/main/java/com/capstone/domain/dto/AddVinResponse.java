package com.capstone.domain.dto;

public record AddVinResponse(String vin, int currentMileage, Long vehicleTypeId, String make, String model, String trim,
        String year, boolean createdVin, boolean createdVehicleType, boolean createdAssociation) {
}