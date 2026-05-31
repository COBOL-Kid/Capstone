package com.capstone.domain;

import com.capstone.models.dto.WarrantyCoverageResponse;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WarrantyStatusCalculator {

  private static final Pattern COVERAGE_VALUE_PATTERN =
      Pattern.compile("^\\s*(\\d+)\\s*/\\s*(.+?)\\s*$", Pattern.CASE_INSENSITIVE);

  private WarrantyStatusCalculator() {}

  public static WarrantyCoverageResponse compute(
      String coverageName,
      String coverageValue,
      int vehicleYear,
      int currentMileage,
      LocalDate today) {
    Optional<ParsedCoverage> parsed = parseCoverageValue(coverageValue);
    if (parsed.isEmpty()) {
      return new WarrantyCoverageResponse(coverageName, coverageValue, null, false, null, null);
    }

    ParsedCoverage coverage = parsed.get();
    LocalDate estimatedExpiration = LocalDate.of(vehicleYear, 1, 1).plusMonths(coverage.months());

    boolean expiredByTime = !today.isBefore(estimatedExpiration);
    boolean expiredByMileage =
        coverage.warrantyMiles() != null && currentMileage >= coverage.warrantyMiles();
    boolean expired = expiredByTime || expiredByMileage;

    Integer remainingMonths = null;
    Integer remainingMiles = null;
    if (!expired) {
      long monthsUntilExpiration = ChronoUnit.MONTHS.between(today, estimatedExpiration);
      remainingMonths = (int) Math.max(0, monthsUntilExpiration);
      if (coverage.warrantyMiles() != null) {
        remainingMiles = Math.max(0, coverage.warrantyMiles() - currentMileage);
      }
    }

    return new WarrantyCoverageResponse(
        coverageName, coverageValue, estimatedExpiration, expired, remainingMonths, remainingMiles);
  }

  private static Optional<ParsedCoverage> parseCoverageValue(String coverageValue) {
    if (coverageValue == null || coverageValue.isBlank()) {
      return Optional.empty();
    }
    Matcher matcher = COVERAGE_VALUE_PATTERN.matcher(coverageValue.trim());
    if (!matcher.matches()) {
      return Optional.empty();
    }
    int months;
    try {
      months = Integer.parseInt(matcher.group(1));
    } catch (NumberFormatException ex) {
      return Optional.empty();
    }
    if (months < 0) {
      return Optional.empty();
    }

    String milesPart = matcher.group(2).trim();
    if (milesPart.equalsIgnoreCase("unlimited")) {
      return Optional.of(new ParsedCoverage(months, null));
    }

    String normalizedMiles = milesPart.replace(",", "");
    try {
      int warrantyMiles = Integer.parseInt(normalizedMiles);
      if (warrantyMiles < 0) {
        return Optional.empty();
      }
      return Optional.of(new ParsedCoverage(months, warrantyMiles));
    } catch (NumberFormatException ex) {
      return Optional.empty();
    }
  }

  private record ParsedCoverage(int months, Integer warrantyMiles) {}
}
