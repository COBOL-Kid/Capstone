package com.capstone.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class VehicleDatabasesPathNormalizerTest {

  @Test
  void shouldReplaceHyphenInModelWithSpace() {
    assertEquals("CX 3", VehicleDatabasesPathNormalizer.normalizeModel("CX-3"));
  }

  @Test
  void shouldLeaveAlphanumericModelUnchanged() {
    assertEquals("4RUNNER", VehicleDatabasesPathNormalizer.normalizeModel("4RUNNER"));
  }

  @Test
  void shouldReplaceHyphenInMakeWithSpace() {
    assertEquals("Mercedes Benz", VehicleDatabasesPathNormalizer.normalizeMake("Mercedes-Benz"));
  }

  @Test
  void shouldCollapseMultipleSpacesAndPunctuation() {
    assertEquals("CX 3", VehicleDatabasesPathNormalizer.normalizeModel("  CX--3  "));
    assertEquals(
        "Silverado 1500", VehicleDatabasesPathNormalizer.normalizeModel("Silverado  1500"));
  }

  @Test
  void shouldPreserveDotInModelAndTrim() {
    assertEquals("2.0T", VehicleDatabasesPathNormalizer.normalizeModel("2.0T"));
    assertEquals("Sport 3.0", VehicleDatabasesPathNormalizer.normalizeTrim("Sport 3.0"));
  }

  @Test
  void shouldBeIdempotentOnAlreadyNormalizedInput() {
    assertEquals("CX 3", VehicleDatabasesPathNormalizer.normalizeModel("CX 3"));
  }

  @Test
  void shouldRejectNullOrBlankSegment() {
    assertThrows(
        IllegalArgumentException.class, () -> VehicleDatabasesPathNormalizer.normalizeModel(null));
    assertThrows(
        IllegalArgumentException.class, () -> VehicleDatabasesPathNormalizer.normalizeModel("  "));
  }
}
