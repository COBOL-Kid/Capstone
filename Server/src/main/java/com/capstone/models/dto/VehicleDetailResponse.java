package com.capstone.models.dto;

import java.util.List;

public record VehicleDetailResponse(
    String vin,
    Long vehicleTypeId,
    String vehicleMake,
    String vehicleModel,
    String vehicleTrim,
    String vehicleYear,
    String vehicleStyle,
    String sourceVin,
    String origin,
    String body,
    String engineDescription,
    String transmissionStyle,
    String driveType,
    String ownersManual,
    int currentMileage,
    List<String> availableImageUrls,
    String selectedImageUrl) {}
