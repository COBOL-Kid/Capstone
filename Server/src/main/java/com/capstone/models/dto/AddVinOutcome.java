package com.capstone.models.dto;

public sealed interface AddVinOutcome {

  record Completed(AddVinResponse response) implements AddVinOutcome {}

  record TrimSelectionRequired(String year, String make, String model) implements AddVinOutcome {}
}
