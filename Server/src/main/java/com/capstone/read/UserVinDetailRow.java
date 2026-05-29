package com.capstone.read;

public record UserVinDetailRow(
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
    String availableImageUrlsJson,
    String selectedImageUrl) {}
