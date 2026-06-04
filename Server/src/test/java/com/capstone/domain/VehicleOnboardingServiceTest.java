package com.capstone.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.capstone.data.*;
import com.capstone.integration.OwnerManualResponse;
import com.capstone.integration.RepairCostResponse;
import com.capstone.integration.RepairEstimatesResponse;
import com.capstone.integration.RepairEstimatesVinProbeResult;
import com.capstone.integration.TrimOptionsResponse;
import com.capstone.integration.VehicleDataProviderClient;
import com.capstone.integration.VehicleDataProviderPrefetchMetrics;
import com.capstone.integration.VehicleDataProviderRequestMetrics;
import com.capstone.integration.VehiclePhotosResponse;
import com.capstone.integration.VehicleRecallsResponse;
import com.capstone.integration.VehicleWarrantyResponse;
import com.capstone.integration.VinDecodeResponse;
import com.capstone.models.*;
import com.capstone.models.dto.AddVinOutcome;
import com.capstone.models.dto.AddVinRequest;
import com.capstone.models.dto.AddVinResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpClientErrorException;

class VehicleOnboardingServiceTest {

  private static final ExecutorService TEST_VEHICLE_DATA_EXECUTOR =
      Executors.newVirtualThreadPerTaskExecutor();

  private static final String PHOTO_1 =
      "https://api.auto.dev/photos/retail/JTENU5JR6M5962554-1.jpg";
  private static final String PHOTO_2 =
      "https://api.auto.dev/photos/retail/JTENU5JR6M5962554-2.jpg";

  @Test
  void shouldLinkExistingVinAndPopulatePhotosWithoutCallingVinDecode() {
    VinRepositoryJPA vinRepository = mock(VinRepositoryJPA.class);
    VehicleTypeRepositoryJPA vehicleTypeRepository = mock(VehicleTypeRepositoryJPA.class);
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    RecallRepositoryJPA recallRepository = mock(RecallRepositoryJPA.class);
    MaintMileageRepositoryJPA maintMileageRepository = mock(MaintMileageRepositoryJPA.class);
    MaintMileageSummaryRepositoryJPA maintMileageSummaryRepository =
        mock(MaintMileageSummaryRepositoryJPA.class);
    MiscMaintCostRepositoryJPA miscMaintCostRepository = mock(MiscMaintCostRepositoryJPA.class);
    VehicleWarrantyRepositoryJPA vehicleWarrantyRepository =
        mock(VehicleWarrantyRepositoryJPA.class);
    VehicleDataProviderClient providerClient = mock(VehicleDataProviderClient.class);
    VehicleDataMapper mapper = new VehicleDataMapper();
    TransactionTemplate transactionTemplate = transactionTemplate();

    VehicleType vehicleType = new VehicleType("Toyota", "4RUNNER", "SRS Prem", "2021");
    vehicleType.setVehicleTypeId(7L);
    Vin vin = new Vin("JTENU5JR6M5962554", vehicleType);
    User user = user();

    when(vinRepository.findById("JTENU5JR6M5962554")).thenReturn(Optional.of(vin));
    when(userVinRepository.findByUserUserIdAndVinVin(1L, "JTENU5JR6M5962554"))
        .thenReturn(Optional.empty());
    when(userVinRepository.save(any(UserVin.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(providerClient.getPhotos("JTENU5JR6M5962554")).thenReturn(photosResponse());
    when(vehicleWarrantyRepository.existsById(new VehicleWarrantyId("2021", "Toyota", "4RUNNER")))
        .thenReturn(true);

    VehicleOnboardingService service =
        onboardingService(
            vinRepository,
            vehicleTypeRepository,
            userVinRepository,
            recallRepository,
            maintMileageRepository,
            maintMileageSummaryRepository,
            miscMaintCostRepository,
            vehicleWarrantyRepository,
            providerClient,
            mapper,
            transactionTemplate);

    var response =
        completed(service.addVinToUser(user, new AddVinRequest("jtenu5jr6m5962554", 45000, null)));

    assertEquals("JTENU5JR6M5962554", response.vin());
    assertEquals(45000, response.currentMileage());
    assertFalse(response.createdVin());
    assertFalse(response.createdVehicleType());
    assertTrue(response.createdAssociation());
    assertEquals(List.of(PHOTO_1, PHOTO_2), response.availableImageUrls());
    assertEquals(PHOTO_1, response.selectedImageUrl());
    verify(providerClient, never()).decodeVin(any());
  }

  @Test
  void shouldReturnExistingAssociationWithoutChangingMileage() {
    VinRepositoryJPA vinRepository = mock(VinRepositoryJPA.class);
    VehicleTypeRepositoryJPA vehicleTypeRepository = mock(VehicleTypeRepositoryJPA.class);
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    RecallRepositoryJPA recallRepository = mock(RecallRepositoryJPA.class);
    MaintMileageRepositoryJPA maintMileageRepository = mock(MaintMileageRepositoryJPA.class);
    MaintMileageSummaryRepositoryJPA maintMileageSummaryRepository =
        mock(MaintMileageSummaryRepositoryJPA.class);
    MiscMaintCostRepositoryJPA miscMaintCostRepository = mock(MiscMaintCostRepositoryJPA.class);
    VehicleWarrantyRepositoryJPA vehicleWarrantyRepository =
        mock(VehicleWarrantyRepositoryJPA.class);
    VehicleDataProviderClient providerClient = mock(VehicleDataProviderClient.class);
    TransactionTemplate transactionTemplate = transactionTemplate();
    User user = user();
    VehicleType vehicleType = new VehicleType("Toyota", "4RUNNER", "SRS Prem", "2021");
    vehicleType.setVehicleTypeId(7L);
    Vin vin = new Vin("JTENU5JR6M5962554", vehicleType);
    UserVin existingAssociation = new UserVin(user, vin, 32000);
    existingAssociation.setAvailableImageUrls(List.of(PHOTO_1));
    existingAssociation.setSelectedImageUrl(PHOTO_1);

    when(vinRepository.findById("JTENU5JR6M5962554")).thenReturn(Optional.of(vin));
    when(userVinRepository.findByUserUserIdAndVinVin(1L, "JTENU5JR6M5962554"))
        .thenReturn(Optional.of(existingAssociation));

    when(vehicleWarrantyRepository.existsById(new VehicleWarrantyId("2021", "Toyota", "4RUNNER")))
        .thenReturn(true);

    VehicleOnboardingService service =
        onboardingService(
            vinRepository,
            vehicleTypeRepository,
            userVinRepository,
            recallRepository,
            maintMileageRepository,
            maintMileageSummaryRepository,
            miscMaintCostRepository,
            vehicleWarrantyRepository,
            providerClient,
            new VehicleDataMapper(),
            transactionTemplate);

    var response =
        completed(
            service.addVinToUser(user, new AddVinRequest(" JTENU5JR6M5962554 ", 45000, null)));

    assertEquals(32000, response.currentMileage());
    assertFalse(response.createdVin());
    assertFalse(response.createdVehicleType());
    assertFalse(response.createdAssociation());
    assertEquals(List.of(PHOTO_1), response.availableImageUrls());
    assertEquals(PHOTO_1, response.selectedImageUrl());
    verify(userVinRepository, never()).save(any(UserVin.class));
    verify(providerClient, never()).decodeVin(any());
    verify(providerClient, never()).getPhotos(any());
  }

  @Test
  void shouldCreateVinForExistingVehicleTypeWithoutFetchingSupplementalData() {
    VinRepositoryJPA vinRepository = mock(VinRepositoryJPA.class);
    VehicleTypeRepositoryJPA vehicleTypeRepository = mock(VehicleTypeRepositoryJPA.class);
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    RecallRepositoryJPA recallRepository = mock(RecallRepositoryJPA.class);
    MaintMileageRepositoryJPA maintMileageRepository = mock(MaintMileageRepositoryJPA.class);
    MaintMileageSummaryRepositoryJPA maintMileageSummaryRepository =
        mock(MaintMileageSummaryRepositoryJPA.class);
    MiscMaintCostRepositoryJPA miscMaintCostRepository = mock(MiscMaintCostRepositoryJPA.class);
    VehicleWarrantyRepositoryJPA vehicleWarrantyRepository =
        mock(VehicleWarrantyRepositoryJPA.class);
    VehicleDataProviderClient providerClient = mock(VehicleDataProviderClient.class);
    TransactionTemplate transactionTemplate = transactionTemplate();
    User user = user();
    VehicleType vehicleType = new VehicleType("Chevrolet", "Silverado 1500", "ZR2", "2022");
    vehicleType.setVehicleStyle("4x4 4dr Crew Cab 5.8 ft. SB");
    vehicleType.setVehicleTypeId(7L);

    when(vinRepository.findById("JTENU5JR6M5962554")).thenReturn(Optional.empty());
    when(providerClient.decodeVin("JTENU5JR6M5962554")).thenReturn(vinDecodeResponse());
    when(vehicleTypeRepository.findByIdentity(
            "2022", "Chevrolet", "Silverado 1500", "ZR2", "4x4 4dr Crew Cab 5.8 ft. SB"))
        .thenReturn(Optional.of(vehicleType));
    when(vinRepository.save(any(Vin.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(userVinRepository.findByUserUserIdAndVinVin(1L, "JTENU5JR6M5962554"))
        .thenReturn(Optional.empty());
    when(userVinRepository.save(any(UserVin.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(providerClient.getPhotos("JTENU5JR6M5962554")).thenReturn(photosResponse());
    when(vehicleWarrantyRepository.existsById(
            new VehicleWarrantyId("2022", "Chevrolet", "Silverado 1500")))
        .thenReturn(false);
    when(providerClient.getVehicleWarranty("2022", "Chevrolet", "Silverado 1500"))
        .thenReturn(vehicleWarrantyResponse());
    when(vehicleWarrantyRepository.save(any(VehicleWarranty.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    VehicleOnboardingService service =
        onboardingService(
            vinRepository,
            vehicleTypeRepository,
            userVinRepository,
            recallRepository,
            maintMileageRepository,
            maintMileageSummaryRepository,
            miscMaintCostRepository,
            vehicleWarrantyRepository,
            providerClient,
            new VehicleDataMapper(),
            transactionTemplate);

    var response =
        completed(service.addVinToUser(user, new AddVinRequest("jtenu5jr6m5962554", 45000, null)));

    assertEquals("JTENU5JR6M5962554", response.vin());
    assertEquals(45000, response.currentMileage());
    assertTrue(response.createdVin());
    assertFalse(response.createdVehicleType());
    assertTrue(response.createdAssociation());
    assertEquals(PHOTO_1, response.selectedImageUrl());
    verify(providerClient).getVehicleWarranty("2022", "Chevrolet", "Silverado 1500");
    verify(vehicleWarrantyRepository).save(any(VehicleWarranty.class));
    verify(providerClient, never()).getOwnerManual(any());
    verify(providerClient, never()).getRepairEstimates(any());
    verify(providerClient, never()).getRepairCosts(any());
    verify(providerClient, never()).getRecalls(any());
    verify(recallRepository, never()).saveAll(any());
    verify(maintMileageRepository, never()).saveAll(any());
    verify(maintMileageSummaryRepository, never()).saveAll(any());
    verify(miscMaintCostRepository, never()).saveAll(any());
  }

  @Test
  void shouldCreateNewVehicleTypeAndPersistSupplementalData() {
    VinRepositoryJPA vinRepository = mock(VinRepositoryJPA.class);
    VehicleTypeRepositoryJPA vehicleTypeRepository = mock(VehicleTypeRepositoryJPA.class);
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    RecallRepositoryJPA recallRepository = mock(RecallRepositoryJPA.class);
    MaintMileageRepositoryJPA maintMileageRepository = mock(MaintMileageRepositoryJPA.class);
    MaintMileageSummaryRepositoryJPA maintMileageSummaryRepository =
        mock(MaintMileageSummaryRepositoryJPA.class);
    MiscMaintCostRepositoryJPA miscMaintCostRepository = mock(MiscMaintCostRepositoryJPA.class);
    VehicleWarrantyRepositoryJPA vehicleWarrantyRepository =
        mock(VehicleWarrantyRepositoryJPA.class);
    VehicleDataProviderClient providerClient = mock(VehicleDataProviderClient.class);
    TransactionTemplate transactionTemplate = transactionTemplate();
    User user = user();

    when(vinRepository.findById("JTENU5JR6M5962554")).thenReturn(Optional.empty());
    when(providerClient.decodeVin("JTENU5JR6M5962554")).thenReturn(newVehicleVinDecodeResponse());
    when(vehicleTypeRepository.findByIdentity("2021", "Toyota", "4RUNNER", "SRS Prem", "SUV"))
        .thenReturn(Optional.empty());
    when(providerClient.getPhotos("JTENU5JR6M5962554")).thenReturn(photosResponse());
    when(providerClient.getOwnerManual("JTENU5JR6M5962554")).thenReturn(ownerManualResponse());
    when(providerClient.probeRepairEstimatesByVin("JTENU5JR6M5962554"))
        .thenReturn(
            new RepairEstimatesVinProbeResult.Found(new RepairEstimatesResponse("success", null)));
    when(providerClient.getRepairCosts("JTENU5JR6M5962554"))
        .thenReturn(new RepairCostResponse("success", null));
    when(providerClient.getRecalls("JTENU5JR6M5962554"))
        .thenReturn(
            new VehicleRecallsResponse(
                "success",
                new VehicleRecallsResponse.VehicleRecallsData(
                    "JTENU5JR6M5962554", "2021", "Toyota", "4RUNNER", List.of())));
    when(providerClient.getVehicleWarranty("2021", "Toyota", "4RUNNER"))
        .thenReturn(vehicleWarrantyResponse());
    when(vehicleWarrantyRepository.existsById(new VehicleWarrantyId("2021", "Toyota", "4RUNNER")))
        .thenReturn(false);
    when(vehicleWarrantyRepository.save(any(VehicleWarranty.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(vehicleTypeRepository.save(any(VehicleType.class)))
        .thenAnswer(
            invocation -> {
              VehicleType saved = invocation.getArgument(0);
              saved.setVehicleTypeId(99L);
              return saved;
            });
    when(vinRepository.save(any(Vin.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(userVinRepository.findByUserUserIdAndVinVin(1L, "JTENU5JR6M5962554"))
        .thenReturn(Optional.empty());
    when(userVinRepository.save(any(UserVin.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    VehicleOnboardingService service =
        onboardingService(
            vinRepository,
            vehicleTypeRepository,
            userVinRepository,
            recallRepository,
            maintMileageRepository,
            maintMileageSummaryRepository,
            miscMaintCostRepository,
            vehicleWarrantyRepository,
            providerClient,
            new VehicleDataMapper(),
            transactionTemplate);

    var response =
        completed(service.addVinToUser(user, new AddVinRequest("jtenu5jr6m5962554", 45000, null)));

    assertEquals("JTENU5JR6M5962554", response.vin());
    assertTrue(response.createdVehicleType());
    assertTrue(response.createdVin());
    assertTrue(response.createdAssociation());
    verify(maintMileageSummaryRepository).saveAll(any());
    verify(maintMileageRepository).saveAll(any());
    verify(miscMaintCostRepository).saveAll(any());
    verify(recallRepository).saveAll(any());
    verify(vehicleWarrantyRepository).save(any(VehicleWarranty.class));
    verify(providerClient).getOwnerManual("JTENU5JR6M5962554");
    verify(providerClient).probeRepairEstimatesByVin("JTENU5JR6M5962554");
    verify(providerClient, never()).getRepairEstimates("JTENU5JR6M5962554");
    verify(providerClient).getRepairCosts("JTENU5JR6M5962554");
    verify(providerClient).getRecalls("JTENU5JR6M5962554");
    verify(providerClient).getVehicleWarranty("2021", "Toyota", "4RUNNER");
  }

  @Test
  void shouldRejectMissingUserAndInvalidMileageBeforeCallingProvider() {
    VinRepositoryJPA vinRepository = mock(VinRepositoryJPA.class);
    VehicleTypeRepositoryJPA vehicleTypeRepository = mock(VehicleTypeRepositoryJPA.class);
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    RecallRepositoryJPA recallRepository = mock(RecallRepositoryJPA.class);
    MaintMileageRepositoryJPA maintMileageRepository = mock(MaintMileageRepositoryJPA.class);
    MaintMileageSummaryRepositoryJPA maintMileageSummaryRepository =
        mock(MaintMileageSummaryRepositoryJPA.class);
    MiscMaintCostRepositoryJPA miscMaintCostRepository = mock(MiscMaintCostRepositoryJPA.class);
    VehicleWarrantyRepositoryJPA vehicleWarrantyRepository =
        mock(VehicleWarrantyRepositoryJPA.class);
    VehicleDataProviderClient providerClient = mock(VehicleDataProviderClient.class);
    VehicleOnboardingService service =
        onboardingService(
            vinRepository,
            vehicleTypeRepository,
            userVinRepository,
            recallRepository,
            maintMileageRepository,
            maintMileageSummaryRepository,
            miscMaintCostRepository,
            vehicleWarrantyRepository,
            providerClient,
            new VehicleDataMapper(),
            transactionTemplate());

    assertEquals(
        "Authenticated user is required",
        assertThrows(
                IllegalArgumentException.class,
                () -> service.addVinToUser(null, new AddVinRequest("JTENU5JR6M5962554", 1, null)))
            .getMessage());
    assertEquals(
        "VIN must be 17 characters",
        assertThrows(
                IllegalArgumentException.class,
                () -> service.addVinToUser(user(), new AddVinRequest("too-short", 1, null)))
            .getMessage());
    assertEquals(
        "Current mileage cannot be negative",
        assertThrows(
                IllegalArgumentException.class,
                () ->
                    service.addVinToUser(user(), new AddVinRequest("JTENU5JR6M5962554", -1, null)))
            .getMessage());
    verify(providerClient, never()).decodeVin(any());
  }

  @Test
  void shouldAllowVehicleOnboardingWhenProviderReturnsNoPhotos() {
    VinRepositoryJPA vinRepository = mock(VinRepositoryJPA.class);
    VehicleTypeRepositoryJPA vehicleTypeRepository = mock(VehicleTypeRepositoryJPA.class);
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    RecallRepositoryJPA recallRepository = mock(RecallRepositoryJPA.class);
    MaintMileageRepositoryJPA maintMileageRepository = mock(MaintMileageRepositoryJPA.class);
    MaintMileageSummaryRepositoryJPA maintMileageSummaryRepository =
        mock(MaintMileageSummaryRepositoryJPA.class);
    MiscMaintCostRepositoryJPA miscMaintCostRepository = mock(MiscMaintCostRepositoryJPA.class);
    VehicleWarrantyRepositoryJPA vehicleWarrantyRepository =
        mock(VehicleWarrantyRepositoryJPA.class);
    VehicleDataProviderClient providerClient = mock(VehicleDataProviderClient.class);
    TransactionTemplate transactionTemplate = transactionTemplate();
    User user = user();
    VehicleType vehicleType = new VehicleType("Toyota", "4RUNNER", "SRS Prem", "2021");
    vehicleType.setVehicleTypeId(7L);
    Vin vin = new Vin("JTENU5JR6M5962554", vehicleType);

    when(vinRepository.findById("JTENU5JR6M5962554")).thenReturn(Optional.of(vin));
    when(userVinRepository.findByUserUserIdAndVinVin(1L, "JTENU5JR6M5962554"))
        .thenReturn(Optional.empty());
    when(userVinRepository.save(any(UserVin.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(providerClient.getPhotos("JTENU5JR6M5962554")).thenReturn(new VehiclePhotosResponse(null));
    when(vehicleWarrantyRepository.existsById(new VehicleWarrantyId("2021", "Toyota", "4RUNNER")))
        .thenReturn(true);

    VehicleOnboardingService service =
        onboardingService(
            vinRepository,
            vehicleTypeRepository,
            userVinRepository,
            recallRepository,
            maintMileageRepository,
            maintMileageSummaryRepository,
            miscMaintCostRepository,
            vehicleWarrantyRepository,
            providerClient,
            new VehicleDataMapper(),
            transactionTemplate);

    var response =
        completed(service.addVinToUser(user, new AddVinRequest("JTENU5JR6M5962554", 45000, null)));

    assertEquals(List.of(), response.availableImageUrls());
    assertNull(response.selectedImageUrl());
    assertTrue(response.createdAssociation());
  }

  @Test
  void shouldRejectInvalidVinDecodeResponseBeforeSaving() {
    VinRepositoryJPA vinRepository = mock(VinRepositoryJPA.class);
    VehicleTypeRepositoryJPA vehicleTypeRepository = mock(VehicleTypeRepositoryJPA.class);
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    RecallRepositoryJPA recallRepository = mock(RecallRepositoryJPA.class);
    MaintMileageRepositoryJPA maintMileageRepository = mock(MaintMileageRepositoryJPA.class);
    MaintMileageSummaryRepositoryJPA maintMileageSummaryRepository =
        mock(MaintMileageSummaryRepositoryJPA.class);
    MiscMaintCostRepositoryJPA miscMaintCostRepository = mock(MiscMaintCostRepositoryJPA.class);
    VehicleWarrantyRepositoryJPA vehicleWarrantyRepository =
        mock(VehicleWarrantyRepositoryJPA.class);
    VehicleDataProviderClient providerClient = mock(VehicleDataProviderClient.class);
    TransactionTemplate transactionTemplate = transactionTemplate();
    User user = user();

    when(vinRepository.findById("JTENU5JR6M5962554")).thenReturn(Optional.empty());
    when(providerClient.decodeVin("JTENU5JR6M5962554")).thenReturn(invalidVinDecodeResponse());

    VehicleOnboardingService service =
        onboardingService(
            vinRepository,
            vehicleTypeRepository,
            userVinRepository,
            recallRepository,
            maintMileageRepository,
            maintMileageSummaryRepository,
            miscMaintCostRepository,
            vehicleWarrantyRepository,
            providerClient,
            new VehicleDataMapper(),
            transactionTemplate);

    assertThrows(
        VinNotFoundException.class,
        () -> service.addVinToUser(user, new AddVinRequest("JTENU5JR6M5962554", 45000, null)));
    verify(vehicleTypeRepository, never()).findByIdentity(any(), any(), any(), any(), any());
    verify(vinRepository, never()).save(any(Vin.class));
  }

  @Test
  void shouldRequireTrimSelectionWhenRepairEstimatesProbeReturns400() {
    VinRepositoryJPA vinRepository = mock(VinRepositoryJPA.class);
    VehicleTypeRepositoryJPA vehicleTypeRepository = mock(VehicleTypeRepositoryJPA.class);
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    RecallRepositoryJPA recallRepository = mock(RecallRepositoryJPA.class);
    MaintMileageRepositoryJPA maintMileageRepository = mock(MaintMileageRepositoryJPA.class);
    MaintMileageSummaryRepositoryJPA maintMileageSummaryRepository =
        mock(MaintMileageSummaryRepositoryJPA.class);
    MiscMaintCostRepositoryJPA miscMaintCostRepository = mock(MiscMaintCostRepositoryJPA.class);
    VehicleWarrantyRepositoryJPA vehicleWarrantyRepository =
        mock(VehicleWarrantyRepositoryJPA.class);
    VehicleDataProviderClient providerClient = mock(VehicleDataProviderClient.class);
    User user = user();

    when(vinRepository.findById("JTENU5JR6M5962554")).thenReturn(Optional.empty());
    when(providerClient.decodeVin("JTENU5JR6M5962554")).thenReturn(newVehicleVinDecodeResponse());
    when(vehicleTypeRepository.findByIdentity("2021", "Toyota", "4RUNNER", "SRS Prem", "SUV"))
        .thenReturn(Optional.empty());
    when(providerClient.getPhotos("JTENU5JR6M5962554")).thenReturn(photosResponse());
    when(providerClient.probeRepairEstimatesByVin("JTENU5JR6M5962554"))
        .thenReturn(new RepairEstimatesVinProbeResult.TrimSelectionRequired());

    VehicleOnboardingService service =
        onboardingService(
            vinRepository,
            vehicleTypeRepository,
            userVinRepository,
            recallRepository,
            maintMileageRepository,
            maintMileageSummaryRepository,
            miscMaintCostRepository,
            vehicleWarrantyRepository,
            providerClient,
            new VehicleDataMapper(),
            transactionTemplate());

    AddVinOutcome outcome =
        service.addVinToUser(user, new AddVinRequest("jtenu5jr6m5962554", 45000, null));

    assertInstanceOf(AddVinOutcome.TrimSelectionRequired.class, outcome);
    var required = (AddVinOutcome.TrimSelectionRequired) outcome;
    assertEquals("2021", required.year());
    assertEquals("Toyota", required.make());
    assertEquals("4RUNNER", required.model());
    verify(vinRepository, never()).save(any(Vin.class));
    verify(providerClient, never()).getOwnerManual(any());
  }

  @Test
  void shouldRequireTrimSelectionWhenPhotosPrefetchFails() {
    VinRepositoryJPA vinRepository = mock(VinRepositoryJPA.class);
    VehicleTypeRepositoryJPA vehicleTypeRepository = mock(VehicleTypeRepositoryJPA.class);
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    RecallRepositoryJPA recallRepository = mock(RecallRepositoryJPA.class);
    MaintMileageRepositoryJPA maintMileageRepository = mock(MaintMileageRepositoryJPA.class);
    MaintMileageSummaryRepositoryJPA maintMileageSummaryRepository =
        mock(MaintMileageSummaryRepositoryJPA.class);
    MiscMaintCostRepositoryJPA miscMaintCostRepository = mock(MiscMaintCostRepositoryJPA.class);
    VehicleWarrantyRepositoryJPA vehicleWarrantyRepository =
        mock(VehicleWarrantyRepositoryJPA.class);
    VehicleDataProviderClient providerClient = mock(VehicleDataProviderClient.class);
    User user = user();

    when(vinRepository.findById("JTENU5JR6M5962554")).thenReturn(Optional.empty());
    when(providerClient.decodeVin("JTENU5JR6M5962554")).thenReturn(newVehicleVinDecodeResponse());
    when(vehicleTypeRepository.findByIdentity("2021", "Toyota", "4RUNNER", "SRS Prem", "SUV"))
        .thenReturn(Optional.empty());
    when(providerClient.getPhotos("JTENU5JR6M5962554"))
        .thenThrow(
            HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", null, null, null));
    when(providerClient.probeRepairEstimatesByVin("JTENU5JR6M5962554"))
        .thenReturn(new RepairEstimatesVinProbeResult.TrimSelectionRequired());

    VehicleOnboardingService service =
        onboardingService(
            vinRepository,
            vehicleTypeRepository,
            userVinRepository,
            recallRepository,
            maintMileageRepository,
            maintMileageSummaryRepository,
            miscMaintCostRepository,
            vehicleWarrantyRepository,
            providerClient,
            new VehicleDataMapper(),
            transactionTemplate());

    AddVinOutcome outcome =
        service.addVinToUser(user, new AddVinRequest("JTENU5JR6M5962554", 45000, null));

    assertInstanceOf(AddVinOutcome.TrimSelectionRequired.class, outcome);
    verify(vinRepository, never()).save(any(Vin.class));
  }

  @Test
  void shouldCompleteOnboardingWithFallbackEndpointsWhenTrimSelected() {
    VinRepositoryJPA vinRepository = mock(VinRepositoryJPA.class);
    VehicleTypeRepositoryJPA vehicleTypeRepository = mock(VehicleTypeRepositoryJPA.class);
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    RecallRepositoryJPA recallRepository = mock(RecallRepositoryJPA.class);
    MaintMileageRepositoryJPA maintMileageRepository = mock(MaintMileageRepositoryJPA.class);
    MaintMileageSummaryRepositoryJPA maintMileageSummaryRepository =
        mock(MaintMileageSummaryRepositoryJPA.class);
    MiscMaintCostRepositoryJPA miscMaintCostRepository = mock(MiscMaintCostRepositoryJPA.class);
    VehicleWarrantyRepositoryJPA vehicleWarrantyRepository =
        mock(VehicleWarrantyRepositoryJPA.class);
    VehicleDataProviderClient providerClient = mock(VehicleDataProviderClient.class);
    User user = user();
    String selectedTrim = "Premium Plus 4dr All-Wheel Drive";

    when(vinRepository.findById("JTENU5JR6M5962554")).thenReturn(Optional.empty());
    when(providerClient.decodeVin("JTENU5JR6M5962554")).thenReturn(newVehicleVinDecodeResponse());
    when(vehicleTypeRepository.findByIdentity("2021", "Toyota", "4RUNNER", selectedTrim, "SUV"))
        .thenReturn(Optional.empty());
    when(providerClient.getPhotos("JTENU5JR6M5962554")).thenReturn(photosResponse());
    when(providerClient.getRepairEstimates("2021", "Toyota", "4RUNNER", selectedTrim))
        .thenReturn(new RepairEstimatesResponse("success", null));
    when(providerClient.getOwnerManual("2021", "Toyota", "4RUNNER"))
        .thenReturn(ownerManualResponse());
    when(providerClient.getRepairCosts("2021", "Toyota", "4RUNNER"))
        .thenReturn(new RepairCostResponse("success", null));
    when(providerClient.getRecalls("2021", "Toyota", "4RUNNER"))
        .thenReturn(
            new VehicleRecallsResponse(
                "success",
                new VehicleRecallsResponse.VehicleRecallsData(
                    null, "2021", "Toyota", "4RUNNER", List.of())));
    when(vehicleTypeRepository.save(any(VehicleType.class)))
        .thenAnswer(
            invocation -> {
              VehicleType saved = invocation.getArgument(0);
              saved.setVehicleTypeId(99L);
              return saved;
            });
    when(vinRepository.save(any(Vin.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(userVinRepository.findByUserUserIdAndVinVin(1L, "JTENU5JR6M5962554"))
        .thenReturn(Optional.empty());
    when(userVinRepository.save(any(UserVin.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    VehicleOnboardingService service =
        onboardingService(
            vinRepository,
            vehicleTypeRepository,
            userVinRepository,
            recallRepository,
            maintMileageRepository,
            maintMileageSummaryRepository,
            miscMaintCostRepository,
            vehicleWarrantyRepository,
            providerClient,
            new VehicleDataMapper(),
            transactionTemplate());

    var response =
        completed(
            service.addVinToUser(
                user, new AddVinRequest("jtenu5jr6m5962554", 45000, selectedTrim)));

    assertEquals(selectedTrim, response.trim());
    verify(providerClient, never()).probeRepairEstimatesByVin(any());
    verify(providerClient).getRepairEstimates("2021", "Toyota", "4RUNNER", selectedTrim);
    verify(providerClient).getOwnerManual("2021", "Toyota", "4RUNNER");
    verify(providerClient).getRepairCosts("2021", "Toyota", "4RUNNER");
    verify(providerClient).getRecalls("2021", "Toyota", "4RUNNER");
    verify(providerClient, never()).getVehicleWarranty(any(), any(), any());
    verify(vehicleWarrantyRepository, never()).save(any());
  }

  @Test
  void shouldTrackPrefetchConcurrencyDuringFallbackOnboarding() throws Exception {
    CountDownLatch allVdbCallsStarted = new CountDownLatch(4);
    CountDownLatch releaseVdbCalls = new CountDownLatch(1);
    CapturingRequestMetrics metrics = new CapturingRequestMetrics();

    VinRepositoryJPA vinRepository = mock(VinRepositoryJPA.class);
    VehicleTypeRepositoryJPA vehicleTypeRepository = mock(VehicleTypeRepositoryJPA.class);
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    RecallRepositoryJPA recallRepository = mock(RecallRepositoryJPA.class);
    MaintMileageRepositoryJPA maintMileageRepository = mock(MaintMileageRepositoryJPA.class);
    MaintMileageSummaryRepositoryJPA maintMileageSummaryRepository =
        mock(MaintMileageSummaryRepositoryJPA.class);
    MiscMaintCostRepositoryJPA miscMaintCostRepository = mock(MiscMaintCostRepositoryJPA.class);
    VehicleWarrantyRepositoryJPA vehicleWarrantyRepository =
        mock(VehicleWarrantyRepositoryJPA.class);
    VehicleDataProviderClient providerClient = mock(VehicleDataProviderClient.class);
    User user = user();
    String selectedTrim = "Premium Plus 4dr All-Wheel Drive";

    when(vinRepository.findById("JTENU5JR6M5962554")).thenReturn(Optional.empty());
    when(providerClient.decodeVin("JTENU5JR6M5962554")).thenReturn(newVehicleVinDecodeResponse());
    when(vehicleTypeRepository.findByIdentity("2021", "Toyota", "4RUNNER", selectedTrim, "SUV"))
        .thenReturn(Optional.empty());
    when(providerClient.getPhotos("JTENU5JR6M5962554")).thenReturn(photosResponse());
    when(providerClient.getRepairEstimates("2021", "Toyota", "4RUNNER", selectedTrim))
        .thenAnswer(
            invocation ->
                blockForPrefetchMetrics(
                    metrics,
                    allVdbCallsStarted,
                    releaseVdbCalls,
                    new RepairEstimatesResponse("success", null)));
    when(providerClient.getOwnerManual("2021", "Toyota", "4RUNNER"))
        .thenAnswer(
            invocation ->
                blockForPrefetchMetrics(
                    metrics, allVdbCallsStarted, releaseVdbCalls, ownerManualResponse()));
    when(providerClient.getRepairCosts("2021", "Toyota", "4RUNNER"))
        .thenAnswer(
            invocation ->
                blockForPrefetchMetrics(
                    metrics,
                    allVdbCallsStarted,
                    releaseVdbCalls,
                    new RepairCostResponse("success", null)));
    when(providerClient.getRecalls("2021", "Toyota", "4RUNNER"))
        .thenAnswer(
            invocation ->
                blockForPrefetchMetrics(
                    metrics,
                    allVdbCallsStarted,
                    releaseVdbCalls,
                    new VehicleRecallsResponse(
                        "success",
                        new VehicleRecallsResponse.VehicleRecallsData(
                            null, "2021", "Toyota", "4RUNNER", List.of()))));
    when(vehicleTypeRepository.save(any(VehicleType.class)))
        .thenAnswer(
            invocation -> {
              VehicleType saved = invocation.getArgument(0);
              saved.setVehicleTypeId(99L);
              return saved;
            });
    when(vinRepository.save(any(Vin.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(userVinRepository.findByUserUserIdAndVinVin(1L, "JTENU5JR6M5962554"))
        .thenReturn(Optional.empty());
    when(userVinRepository.save(any(UserVin.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    VehicleOnboardingService service =
        onboardingService(
            vinRepository,
            vehicleTypeRepository,
            userVinRepository,
            recallRepository,
            maintMileageRepository,
            maintMileageSummaryRepository,
            miscMaintCostRepository,
            vehicleWarrantyRepository,
            providerClient,
            new VehicleDataMapper(),
            metrics,
            transactionTemplate());

    Thread onboardingThread =
        new Thread(
            () ->
                completed(
                    service.addVinToUser(
                        user, new AddVinRequest("jtenu5jr6m5962554", 45000, selectedTrim))));
    onboardingThread.start();

    assertTrue(allVdbCallsStarted.await(5, TimeUnit.SECONDS));
    assertNotNull(metrics.lastPrefetch());
    assertTrue(
        metrics.lastPrefetch().maxInFlight() >= 4,
        "expected fallback prefetch to track parallel VDB calls, peak="
            + metrics.lastPrefetch().maxInFlight());

    releaseVdbCalls.countDown();
    onboardingThread.join(10_000);
  }

  @Test
  void shouldRequireTrimSelectionWithAutoDevModelName() {
    VinRepositoryJPA vinRepository = mock(VinRepositoryJPA.class);
    VehicleTypeRepositoryJPA vehicleTypeRepository = mock(VehicleTypeRepositoryJPA.class);
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    RecallRepositoryJPA recallRepository = mock(RecallRepositoryJPA.class);
    MaintMileageRepositoryJPA maintMileageRepository = mock(MaintMileageRepositoryJPA.class);
    MaintMileageSummaryRepositoryJPA maintMileageSummaryRepository =
        mock(MaintMileageSummaryRepositoryJPA.class);
    MiscMaintCostRepositoryJPA miscMaintCostRepository = mock(MiscMaintCostRepositoryJPA.class);
    VehicleWarrantyRepositoryJPA vehicleWarrantyRepository =
        mock(VehicleWarrantyRepositoryJPA.class);
    VehicleDataProviderClient providerClient = mock(VehicleDataProviderClient.class);
    User user = user();

    when(vinRepository.findById("JM1DKDB77M1234567")).thenReturn(Optional.empty());
    when(providerClient.decodeVin("JM1DKDB77M1234567")).thenReturn(mazdaCx3VinDecodeResponse());
    when(vehicleTypeRepository.findByIdentity("2019", "Mazda", "CX-3", "Sport", "SUV"))
        .thenReturn(Optional.empty());
    when(providerClient.getPhotos("JM1DKDB77M1234567")).thenReturn(photosResponse());
    when(providerClient.probeRepairEstimatesByVin("JM1DKDB77M1234567"))
        .thenReturn(new RepairEstimatesVinProbeResult.TrimSelectionRequired());

    VehicleOnboardingService service =
        onboardingService(
            vinRepository,
            vehicleTypeRepository,
            userVinRepository,
            recallRepository,
            maintMileageRepository,
            maintMileageSummaryRepository,
            miscMaintCostRepository,
            vehicleWarrantyRepository,
            providerClient,
            new VehicleDataMapper(),
            transactionTemplate());

    AddVinOutcome outcome =
        service.addVinToUser(user, new AddVinRequest("jm1dkdb77m1234567", 45000, null));

    var required = (AddVinOutcome.TrimSelectionRequired) outcome;
    assertEquals("2019", required.year());
    assertEquals("Mazda", required.make());
    assertEquals("CX-3", required.model());
  }

  @Test
  void shouldReturnTrimOptionsFromProvider() {
    VehicleDataProviderClient providerClient = mock(VehicleDataProviderClient.class);
    when(providerClient.getTrimOptions("2021", "Toyota", "4RUNNER"))
        .thenReturn(
            new TrimOptionsResponse(
                "success",
                new TrimOptionsResponse.Data(
                    "2021",
                    "Toyota",
                    "4RUNNER",
                    List.of("SRS Prem", "Limited 4dr SUV (2.7L 4cyl 4A)"))));

    VehicleOnboardingService service =
        onboardingService(
            mock(VinRepositoryJPA.class),
            mock(VehicleTypeRepositoryJPA.class),
            mock(UserVinRepositoryJPA.class),
            mock(RecallRepositoryJPA.class),
            mock(MaintMileageRepositoryJPA.class),
            mock(MaintMileageSummaryRepositoryJPA.class),
            mock(MiscMaintCostRepositoryJPA.class),
            mock(VehicleWarrantyRepositoryJPA.class),
            providerClient,
            new VehicleDataMapper(),
            transactionTemplate());

    assertEquals(
        List.of("SRS Prem", "Limited 4dr SUV (2.7L 4cyl 4A)"),
        service.getTrimOptions("2021", "Toyota", "4RUNNER"));
  }

  private static AddVinResponse completed(AddVinOutcome outcome) {
    assertInstanceOf(AddVinOutcome.Completed.class, outcome);
    return ((AddVinOutcome.Completed) outcome).response();
  }

  private VehicleOnboardingService onboardingService(
      VinRepositoryJPA vinRepository,
      VehicleTypeRepositoryJPA vehicleTypeRepository,
      UserVinRepositoryJPA userVinRepository,
      RecallRepositoryJPA recallRepository,
      MaintMileageRepositoryJPA maintMileageRepository,
      MaintMileageSummaryRepositoryJPA maintMileageSummaryRepository,
      MiscMaintCostRepositoryJPA miscMaintCostRepository,
      VehicleWarrantyRepositoryJPA vehicleWarrantyRepository,
      VehicleDataProviderClient providerClient,
      VehicleDataMapper mapper,
      TransactionTemplate transactionTemplate) {
    return onboardingService(
        vinRepository,
        vehicleTypeRepository,
        userVinRepository,
        recallRepository,
        maintMileageRepository,
        maintMileageSummaryRepository,
        miscMaintCostRepository,
        vehicleWarrantyRepository,
        providerClient,
        mapper,
        new VehicleDataProviderRequestMetrics(),
        transactionTemplate);
  }

  private VehicleOnboardingService onboardingService(
      VinRepositoryJPA vinRepository,
      VehicleTypeRepositoryJPA vehicleTypeRepository,
      UserVinRepositoryJPA userVinRepository,
      RecallRepositoryJPA recallRepository,
      MaintMileageRepositoryJPA maintMileageRepository,
      MaintMileageSummaryRepositoryJPA maintMileageSummaryRepository,
      MiscMaintCostRepositoryJPA miscMaintCostRepository,
      VehicleWarrantyRepositoryJPA vehicleWarrantyRepository,
      VehicleDataProviderClient providerClient,
      VehicleDataMapper mapper,
      VehicleDataProviderRequestMetrics metrics,
      TransactionTemplate transactionTemplate) {
    return new VehicleOnboardingService(
        vinRepository,
        vehicleTypeRepository,
        userVinRepository,
        recallRepository,
        maintMileageRepository,
        maintMileageSummaryRepository,
        miscMaintCostRepository,
        vehicleWarrantyRepository,
        providerClient,
        mapper,
        metrics,
        transactionTemplate,
        TEST_VEHICLE_DATA_EXECUTOR);
  }

  private VehicleWarrantyResponse vehicleWarrantyResponse() {
    Map<String, String> warranty = new LinkedHashMap<>();
    warranty.put("Warranty - Basic (months/miles)", "36/36,000");
    return new VehicleWarrantyResponse(
        "success", new VehicleWarrantyResponse.WarrantyData("2021", "Toyota", "4RUNNER", warranty));
  }

  private TransactionTemplate transactionTemplate() {
    TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
    when(transactionTemplate.execute(any()))
        .thenAnswer(
            invocation ->
                invocation.getArgument(0, TransactionCallback.class).doInTransaction(null));
    return transactionTemplate;
  }

  private User user() {
    User user = new User();
    user.setUserId(1L);
    user.setUserEmail("driver@example.com");
    user.setRole(Role.USER);
    return user;
  }

  private static Object blockForPrefetchMetrics(
      VehicleDataProviderRequestMetrics metrics,
      CountDownLatch allVdbCallsStarted,
      CountDownLatch releaseVdbCalls,
      Object response)
      throws InterruptedException {
    metrics.requestStarted();
    allVdbCallsStarted.countDown();
    try {
      releaseVdbCalls.await(5, TimeUnit.SECONDS);
      return response;
    } finally {
      metrics.requestFinished();
    }
  }

  private static final class CapturingRequestMetrics extends VehicleDataProviderRequestMetrics {
    private volatile VehicleDataProviderPrefetchMetrics lastPrefetch;

    @Override
    public ScopedValue.Carrier bindPrefetchMetrics(VehicleDataProviderPrefetchMetrics prefetch) {
      lastPrefetch = prefetch;
      return super.bindPrefetchMetrics(prefetch);
    }

    VehicleDataProviderPrefetchMetrics lastPrefetch() {
      return lastPrefetch;
    }
  }

  private VinDecodeResponse invalidVinDecodeResponse() {
    return new VinDecodeResponse(
        "JTENU5JR6M5962554",
        false,
        "JTE",
        "Japan",
        "JTENU5JRM5",
        "6",
        true,
        "Active",
        "Toyota",
        "4RUNNER",
        "SRS Prem",
        "SUV",
        "SUV",
        "4.0L V6 DOHC 24V",
        "4WD",
        "Automatic",
        new VinDecodeResponse.Vehicle(
            "JTENU5JR6M5962554", 2021, "Toyota", "4RUNNER", "Toyota Motor Corporation"),
        new VinDecodeResponse.Photos(false, false, false, 0),
        false);
  }

  private VinDecodeResponse mazdaCx3VinDecodeResponse() {
    return new VinDecodeResponse(
        "JM1DKDB77M1234567",
        true,
        "JM1",
        "Japan",
        "JM1DKDB7M1",
        "7",
        true,
        "Active",
        "Mazda",
        "CX-3",
        "Sport",
        "SUV",
        "SUV",
        "2.0L I4",
        "FWD",
        "Automatic",
        new VinDecodeResponse.Vehicle(
            "JM1DKDB77M1234567", 2019, "Mazda", "CX-3", "Mazda Motor Corporation"),
        new VinDecodeResponse.Photos(true, false, true, 2),
        false);
  }

  private VinDecodeResponse newVehicleVinDecodeResponse() {
    return new VinDecodeResponse(
        "JTENU5JR6M5962554",
        true,
        "JTE",
        "Japan",
        "JTENU5JRM5",
        "6",
        true,
        "Active",
        "Toyota",
        "4RUNNER",
        "SRS Prem",
        "SUV",
        "SUV",
        "4.0L V6 DOHC 24V",
        "4WD",
        "Automatic",
        new VinDecodeResponse.Vehicle(
            "JTENU5JR6M5962554", 2021, "Toyota", "4RUNNER", "Toyota Motor Corporation"),
        new VinDecodeResponse.Photos(true, false, true, 2),
        false);
  }

  private OwnerManualResponse ownerManualResponse() {
    return new OwnerManualResponse(
        "success",
        "JTENU5JR6M5962554",
        new OwnerManualResponse.OwnerManualData(
            null, "2021", "Toyota", "4runner", "https://example.com/manual.pdf"));
  }

  private VinDecodeResponse vinDecodeResponse() {
    return new VinDecodeResponse(
        "3GCUDHEL3NG668790",
        true,
        "3GC",
        "Mexico",
        "3GCUDHELNG",
        "3",
        true,
        "Active",
        "Chevrolet",
        "Silverado 1500",
        "ZR2",
        "4x4 4dr Crew Cab 5.8 ft. SB",
        "Truck",
        "5.3L V8 OHV 16V FFV",
        "4WD",
        "Automatic",
        new VinDecodeResponse.Vehicle(
            "3GCUDHEL3NG668790", 2022, "Chevrolet", "Silverado 1500", "General Motors de Mexico"),
        new VinDecodeResponse.Photos(true, false, true, 12),
        false);
  }

  private VehiclePhotosResponse photosResponse() {
    return new VehiclePhotosResponse(
        new VehiclePhotosResponse.PhotoData(List.of(PHOTO_1, PHOTO_2)));
  }
}
