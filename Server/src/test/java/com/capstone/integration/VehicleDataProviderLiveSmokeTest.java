package com.capstone.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.capstone.domain.VehicleOnboardingService;
import com.capstone.integration.testsupport.VehicleDataFixtures;
import com.capstone.models.Role;
import com.capstone.models.User;
import com.capstone.models.dto.AddVinRequest;
import com.capstone.models.dto.AddVinResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@Tag("live")
@EnabledIfEnvironmentVariable(named = "RUN_VEHICLE_DATA_LIVE_TESTS", matches = "true")
@SpringBootTest
class VehicleDataProviderLiveSmokeTest {

  private static final Logger log = LoggerFactory.getLogger(VehicleDataProviderLiveSmokeTest.class);
  private static final String LIVE_VIN = "3GCUDHEL3NG668790";

  @Autowired private VehicleDataProviderClient vehicleDataProviderClient;
  @Autowired private VehicleOnboardingService vehicleOnboardingService;

  @Value("${vehicle-data.autodev.api-key:}")
  private String autoDevApiKey;

  @Value("${vehicle-data.vehicle-databases.api-key:}")
  private String vehicleDatabasesApiKey;

  @Test
  void shouldDecodeVinFromAutoDev() {
    assumeApiKeysPresent();

    VinDecodeResponse response = vehicleDataProviderClient.decodeVin(LIVE_VIN);

    assertNotNull(response);
    assertTrue(response.vinValid());
    log.info(
        "Live decode succeeded vin={} year={} make={} model={}",
        response.vin(),
        response.vehicle() != null ? response.vehicle().year() : null,
        response.make(),
        response.model());
  }

  @Test
  void shouldFetchRepairEstimatesFromVehicleDatabases() {
    assumeApiKeysPresent();

    RepairEstimatesResponse response = vehicleDataProviderClient.getRepairEstimates(LIVE_VIN);

    assertNotNull(response);
    assertEquals("success", response.status());
    log.info("Live repair estimates succeeded vin={}", LIVE_VIN);
  }

  @Test
  void shouldFetchPhotosFromAutoDev() {
    assumeApiKeysPresent();

    VehiclePhotosResponse response = vehicleDataProviderClient.getPhotos(LIVE_VIN);

    if (response != null) {
      log.info("Live photos returned count={}", response.retailPhotos().size());
    } else {
      log.info("Live photos returned no data for vin={}", LIVE_VIN);
    }
  }

  @Test
  void shouldOnboardUnusedVinEndToEnd() {
    assumeApiKeysPresent();
    String vin = VehicleDataFixtures.BURST_TEST_VIN;

    long startMs = System.currentTimeMillis();
    AddVinResponse response =
        vehicleOnboardingService.addVinToUser(liveUser(), new AddVinRequest(vin, 12_000));
    long durationMs = System.currentTimeMillis() - startMs;

    assertNotNull(response);
    assertEquals(vin, response.vin());
    log.info(
        "Live onboarding completed vin={} createdVehicleType={} createdVin={} durationMs={}",
        response.vin(),
        response.createdVehicleType(),
        response.createdVin(),
        durationMs);
  }

  private void assumeApiKeysPresent() {
    assumeTrue(
        autoDevApiKey != null
            && !autoDevApiKey.isBlank()
            && vehicleDatabasesApiKey != null
            && !vehicleDatabasesApiKey.isBlank(),
        "AUTODEV_API_KEY and VEHICLE_DATA_API_KEY must be set for live provider tests");
  }

  private static User liveUser() {
    User user = new User();
    user.setUserId(1L);
    user.setUserEmail("driver@example.com");
    user.setRole(Role.USER);
    return user;
  }
}
