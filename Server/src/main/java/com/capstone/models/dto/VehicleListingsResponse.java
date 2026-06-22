package com.capstone.models.dto;

import java.util.List;

public record VehicleListingsResponse(
    String vin,
    String year,
    String make,
    String model,
    int page,
    Integer total,
    List<VehicleListingResponse> listings) {}
