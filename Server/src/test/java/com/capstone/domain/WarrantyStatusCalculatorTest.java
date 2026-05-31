package com.capstone.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.capstone.models.dto.WarrantyCoverageResponse;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class WarrantyStatusCalculatorTest {

  private static final String COVERAGE_NAME = "Warranty - Basic (months/miles)";

  @Test
  void shouldComputeActiveBasicWarranty() {
    WarrantyCoverageResponse result =
        WarrantyStatusCalculator.compute(
            COVERAGE_NAME, "36/36,000", 2021, 15_000, LocalDate.of(2023, 6, 1));

    assertEquals(LocalDate.of(2024, 1, 1), result.estimatedExpirationDate());
    assertFalse(result.expired());
    assertEquals(7, result.remainingMonths());
    assertEquals(21_000, result.remainingMiles());
    assertEquals("36/36,000", result.coverageValue());
  }

  @Test
  void shouldExpireBasicWarrantyByMileage() {
    WarrantyCoverageResponse result =
        WarrantyStatusCalculator.compute(
            COVERAGE_NAME, "36/36,000", 2021, 36_000, LocalDate.of(2023, 6, 1));

    assertTrue(result.expired());
    assertNull(result.remainingMonths());
    assertNull(result.remainingMiles());
    assertEquals(LocalDate.of(2024, 1, 1), result.estimatedExpirationDate());
  }

  @Test
  void shouldExpireBasicWarrantyByTime() {
    WarrantyCoverageResponse result =
        WarrantyStatusCalculator.compute(
            COVERAGE_NAME, "36/36,000", 2018, 10_000, LocalDate.of(2026, 5, 30));

    assertTrue(result.expired());
    assertEquals(LocalDate.of(2021, 1, 1), result.estimatedExpirationDate());
    assertNull(result.remainingMonths());
    assertNull(result.remainingMiles());
  }

  @Test
  void shouldHandleUnlimitedMileageCoverage() {
    WarrantyCoverageResponse result =
        WarrantyStatusCalculator.compute(
            "Warranty - Corrosion perforation (months/miles)",
            "60/ unlimited",
            2021,
            200_000,
            LocalDate.of(2023, 6, 1));

    assertEquals(LocalDate.of(2026, 1, 1), result.estimatedExpirationDate());
    assertFalse(result.expired());
    assertEquals(31, result.remainingMonths());
    assertNull(result.remainingMiles());
  }

  @Test
  void shouldReturnUncomputedFieldsForMalformedCoverageValue() {
    WarrantyCoverageResponse result =
        WarrantyStatusCalculator.compute(
            COVERAGE_NAME, "not-a-warranty-value", 2021, 10_000, LocalDate.of(2023, 6, 1));

    assertNull(result.estimatedExpirationDate());
    assertFalse(result.expired());
    assertNull(result.remainingMonths());
    assertNull(result.remainingMiles());
    assertEquals("not-a-warranty-value", result.coverageValue());
  }
}
