package com.capstone.integration;

public sealed interface RepairEstimatesVinProbeResult {

  record Found(RepairEstimatesResponse response) implements RepairEstimatesVinProbeResult {}

  record TrimSelectionRequired() implements RepairEstimatesVinProbeResult {}
}
