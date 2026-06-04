package com.capstone.domain;

import java.util.regex.Pattern;

public final class VehicleDatabasesPathNormalizer {

  private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-zA-Z0-9.]+");

  private VehicleDatabasesPathNormalizer() {}

  public static String normalizeSegment(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Path segment is required");
    }
    return NON_ALPHANUMERIC.matcher(value.trim()).replaceAll(" ").trim();
  }

  public static String normalizeYear(String year) {
    return normalizeSegment(year);
  }

  public static String normalizeMake(String make) {
    return normalizeSegment(make);
  }

  public static String normalizeModel(String model) {
    return normalizeSegment(model);
  }

  public static String normalizeTrim(String trim) {
    return normalizeSegment(trim);
  }
}
