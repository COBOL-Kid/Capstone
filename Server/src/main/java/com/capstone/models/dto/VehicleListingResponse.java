package com.capstone.models.dto;

public record VehicleListingResponse(
    String vin,
    String createdAt,
    String year,
    String make,
    String model,
    String style,
    Integer price,
    Integer miles,
    String dealer,
    String city,
    String state,
    String zip,
    String primaryImage,
    String vdp,
    String carfaxUrl,
    Boolean used,
    Boolean cpo,
    Integer photoCount,
    Double latitude,
    Double longitude,
    VehicleListingHistoryResponse history) {}
