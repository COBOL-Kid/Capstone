package com.capstone.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.capstone.data.UserRepositoryJPA;
import com.capstone.domain.VehicleOnboardingService;
import com.capstone.integration.testsupport.VehicleDataFixtures;
import com.capstone.models.Role;
import com.capstone.models.User;
import com.capstone.models.dto.AddVinOutcome;
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

  private static final String LIVE_USER_EMAIL = "vehicle-data-live@example.com";

  @Autowired private VehicleDataProviderClient vehicleDataProviderClient;
  @Autowired private VehicleOnboardingService vehicleOnboardingService;
  @Autowired private UserRepositoryJPA userRepository;

  @Value("${vehicle-data.autodev.api-key}")
  private String autoDevApiKey;

  @Value("${vehicle-data.vehicle-databases.api-key}")
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
  void shouldFetchTrimOptionsFromVehicleDatabases() {
    assumeApiKeysPresent();

    TrimOptionsResponse response =
        vehicleDataProviderClient.getTrimOptions("2021", "Toyota", "4RUNNER");

    assertNotNull(response);
    assertEquals("success", response.status());
    assertNotNull(response.data());
    assertNotNull(response.data().trims());
    assertFalse(response.data().trims().isEmpty());
    log.info(
        "Live trim options count={} sample={}",
        response.data().trims().size(),
        response.data().trims().getFirst());
  }

  @Test
  void shouldOnboardVinWhenProviderRequiresTrimSelection() {
    assumeApiKeysPresent();
    String vin = VehicleDataFixtures.BURST_TEST_VIN;

    RepairEstimatesVinProbeResult probe = vehicleDataProviderClient.probeRepairEstimatesByVin(vin);
    if (!(probe instanceof RepairEstimatesVinProbeResult.TrimSelectionRequired)) {
      log.info("Live probe did not require trim for vin={}, skipping trim onboarding branch", vin);
      return;
    }

    VinDecodeResponse decode = vehicleDataProviderClient.decodeVin(vin);
    assertNotNull(decode.vehicle());
    String year = String.valueOf(decode.vehicle().year());
    String make = decode.make();
    String model = decode.model();

    var trims = vehicleOnboardingService.getTrimOptions(year, make, model);
    assumeTrue(!trims.isEmpty(), "trim options required to complete live trim onboarding");

    AddVinOutcome outcome =
        vehicleOnboardingService.addVinToUser(
            liveUser(), new AddVinRequest(vin, 12_000, trims.getFirst()));
    assertInstanceOf(AddVinOutcome.Completed.class, outcome);
    AddVinResponse response = ((AddVinOutcome.Completed) outcome).response();
    assertEquals(vin, response.vin());
    assertEquals(trims.getFirst(), response.trim());
    log.info(
        "Live trim onboarding completed vin={} trim={} createdVin={}",
        response.vin(),
        response.trim(),
        response.createdVin());
  }

  @Test
  void shouldOnboardUnusedVinEndToEnd() {
    assumeApiKeysPresent();
    String vin = VehicleDataFixtures.BURST_TEST_VIN;

    long startMs = System.currentTimeMillis();
    AddVinOutcome outcome =
        vehicleOnboardingService.addVinToUser(liveUser(), new AddVinRequest(vin, 12_000, null));
    assertInstanceOf(AddVinOutcome.Completed.class, outcome);
    AddVinResponse response = ((AddVinOutcome.Completed) outcome).response();
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
            && !"test-autodev".equals(autoDevApiKey)
            && vehicleDatabasesApiKey != null
            && !vehicleDatabasesApiKey.isBlank()
            && !"test-vdb".equals(vehicleDatabasesApiKey),
        "AUTODEV_API_KEY and VEHICLE_DATA_API_KEY must be set for live provider tests (source ../.env)");
  }

  private User liveUser() {
    return userRepository
        .findByUserEmail(LIVE_USER_EMAIL)
        .orElseGet(
            () -> {
              User user = new User();
              user.setUserEmail(LIVE_USER_EMAIL);
              user.setUserPw("live-test-unused");
              user.setRole(Role.USER);
              return userRepository.save(user);
            });
  }
}
