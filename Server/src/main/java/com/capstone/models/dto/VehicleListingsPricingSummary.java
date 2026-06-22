package com.capstone.models.dto;

public record VehicleListingsPricingSummary(
    Integer minPrice, Integer maxPrice, Integer averagePrice, int pricedListingCount) {}
