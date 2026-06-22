package com.capstone.models.dto;

public record VehicleListingHistoryResponse(
    Boolean accidents,
    Integer accidentCount,
    Boolean oneOwner,
    Integer ownerCount,
    String usageType) {}
