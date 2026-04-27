package com.capstone.integration.vehicledatabases;

public record OwnerManualResponse(String status, OwnerManualData data) {

    public record OwnerManualData(String vin, String year, String make, String model, String path) {
    }
}