package com.capstone.integration.vehicledatabases;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VinDecodeResponse(String status, VinDecodeData data) {

    public record VinDecodeData(Intro intro, Basic basic, Engine engine, Manufacturer manufacturer,
            Transmission transmission, Restraint restraint, Dimensions dimensions, Drivetrain drivetrain, Fuel fuel) {
    }

    public record Intro(String vin) {
    }

    public record Basic(String make, String model, String year, String trim, @JsonProperty("body_type") String bodyType,
            @JsonProperty("vehicle_type") String vehicleType, String doors,
            @JsonProperty("vehicle_size") String vehicleSize,
            @JsonProperty("seating_capacity") String seatingCapacity) {
    }

    public record Engine(String cylinders, @JsonProperty("engine_size") String engineSize,
            @JsonProperty("engine_description") String engineDescription,
            @JsonProperty("engine_capacity") String engineCapacity,
            @JsonProperty("engine_configuration") String engineConfiguration,
            @JsonProperty("electrification_level") String electrificationLevel) {
    }

    public record Manufacturer(String manufacturer, String region, String country,
            @JsonProperty("plant_city") String plantCity) {
    }

    public record Transmission(@JsonProperty("transmission_style") String transmissionStyle) {
    }

    public record Restraint(String others) {
    }

    public record Dimensions(String gvwr) {
    }

    public record Drivetrain(@JsonProperty("drive_type") String driveType) {
    }

    public record Fuel(@JsonProperty("fuel_type") String fuelType,
            @JsonProperty("secondary_fuel_type") String secondaryFuelType) {
    }
}