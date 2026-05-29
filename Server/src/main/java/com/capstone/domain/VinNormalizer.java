package com.capstone.domain;

public final class VinNormalizer {

  private VinNormalizer() {}

  public static String normalize(String vin) {
    if (vin == null || vin.isBlank()) {
      throw new IllegalArgumentException("VIN is required");
    }
    String normalizedVin = vin.trim().toUpperCase();
    if (normalizedVin.length() != 17) {
      throw new IllegalArgumentException("VIN must be 17 characters");
    }
    return normalizedVin;
  }
}
