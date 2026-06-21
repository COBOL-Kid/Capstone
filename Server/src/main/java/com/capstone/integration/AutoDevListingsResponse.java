package com.capstone.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AutoDevListingsResponse(Integer total, List<Listing> data) {

  public List<Listing> listings() {
    return data == null ? List.of() : data;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Listing(
      @JsonProperty("@id") String id,
      String vin,
      String createdAt,
      List<Double> location,
      Vehicle vehicle,
      RetailListing retailListing,
      History history) {

    public Double longitude() {
      return location != null && location.size() > 0 ? location.get(0) : null;
    }

    public Double latitude() {
      return location != null && location.size() > 1 ? location.get(1) : null;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Vehicle(
      String vin,
      Integer year,
      String make,
      String model,
      String style,
      String bodyStyle,
      String engine,
      String drivetrain,
      String transmission,
      String exteriorColor,
      String interiorColor) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record RetailListing(
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
      Integer photoCount) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record History(
      Boolean accidents,
      Integer accidentCount,
      Boolean oneOwner,
      Integer ownerCount,
      String usageType) {}
}
