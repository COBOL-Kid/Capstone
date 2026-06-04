package com.capstone.models.dto;

public record AddVinTrimSelectionRequiredResponse(
    boolean requiresTrimSelection, String year, String make, String model) {

  public static AddVinTrimSelectionRequiredResponse of(String year, String make, String model) {
    return new AddVinTrimSelectionRequiredResponse(true, year, make, model);
  }
}
