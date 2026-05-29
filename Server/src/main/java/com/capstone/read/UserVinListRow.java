package com.capstone.read;

public record UserVinListRow(
    String vin,
    int currentMileage,
    Long vehicleTypeId,
    String make,
    String model,
    String trim,
    String year,
    String selectedImageUrl) {}
