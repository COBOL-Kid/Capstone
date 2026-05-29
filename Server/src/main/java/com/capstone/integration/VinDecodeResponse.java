package com.capstone.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VinDecodeResponse(
    String vin,
    Boolean vinValid,
    String wmi,
    String origin,
    String squishVin,
    String checkDigit,
    Boolean checksum,
    String type,
    String make,
    String model,
    String trim,
    String style,
    String body,
    String engine,
    String drive,
    String transmission,
    Vehicle vehicle,
    Photos photos,
    Boolean ambiguous) {

  public record Vehicle(String vin, Integer year, String make, String model, String manufacturer) {}

  public record Photos(
      Boolean hasRetailPhotos,
      Boolean hasWholesalePhotos,
      Boolean hasHistoricalPhotos,
      Integer retailPhotoCount) {}
}
