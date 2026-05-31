package com.capstone.models.dto;

import java.util.List;

public record VehicleWarrantyResponse(
    String vehicleYear,
    String vehicleMake,
    String vehicleModel,
    List<WarrantyCoverageResponse> coverages) {}
