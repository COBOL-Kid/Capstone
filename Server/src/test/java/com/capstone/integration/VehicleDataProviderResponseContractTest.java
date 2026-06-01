package com.capstone.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.capstone.domain.VehicleDataMapper;
import com.capstone.integration.testsupport.VehicleDataFixtures;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class VehicleDataProviderResponseContractTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final VehicleDataMapper vehicleDataMapper = new VehicleDataMapper();

  @Test
  void shouldDeserializeVinDecodeFixtureAndMapIdentity() throws Exception {
    VinDecodeResponse response =
        objectMapper.readValue(
            VehicleDataFixtures.read("autodev-vin-decode.json"), VinDecodeResponse.class);

    assertEquals(VehicleDataFixtures.BURST_TEST_VIN, response.vin());
    assertTrue(response.vinValid());
    assertNotNull(response.vehicle());
    assertEquals(2021, response.vehicle().year());

    VehicleDataMapper.VehicleIdentity identity = vehicleDataMapper.toVehicleIdentity(response);
    assertEquals("2021", identity.year());
    assertEquals("Toyota", identity.make());
    assertEquals("4RUNNER", identity.model());
  }

  @Test
  void shouldDeserializeAllProviderFixtures() throws Exception {
    RepairEstimatesResponse repairEstimates =
        objectMapper.readValue(
            VehicleDataFixtures.read("vdb-repair-estimates.json"), RepairEstimatesResponse.class);
    RepairCostResponse repairCosts =
        objectMapper.readValue(
            VehicleDataFixtures.read("vdb-repair-costs.json"), RepairCostResponse.class);
    VehicleRecallsResponse recalls =
        objectMapper.readValue(
            VehicleDataFixtures.read("vdb-vehicle-recalls.json"), VehicleRecallsResponse.class);
    OwnerManualResponse ownerManual =
        objectMapper.readValue(
            VehicleDataFixtures.read("vdb-owner-manual.json"), OwnerManualResponse.class);
    VehicleWarrantyResponse warranty =
        objectMapper.readValue(
            VehicleDataFixtures.read("vdb-vehicle-warranty.json"), VehicleWarrantyResponse.class);
    VehiclePhotosResponse photos =
        objectMapper.readValue(
            VehicleDataFixtures.read("autodev-photos.json"), VehiclePhotosResponse.class);

    assertEquals("success", repairEstimates.status());
    assertEquals("success", repairCosts.status());
    assertEquals("success", recalls.status());
    assertEquals("success", ownerManual.status());
    assertEquals("success", warranty.status());
    assertEquals(2, photos.retailPhotos().size());
    assertNotNull(repairEstimates.data());
    assertNotNull(warranty.data().warranty());
  }
}
