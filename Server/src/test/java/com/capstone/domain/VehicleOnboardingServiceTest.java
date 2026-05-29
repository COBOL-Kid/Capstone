package com.capstone.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.capstone.data.*;
import com.capstone.integration.VehicleDataProviderClient;
import com.capstone.integration.VehiclePhotosResponse;
import com.capstone.integration.VinDecodeResponse;
import com.capstone.models.*;
import com.capstone.models.dto.AddVinRequest;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

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

    VehicleOnboardingService service =
        new VehicleOnboardingService(
            vinRepository,
            vehicleTypeRepository,
            userVinRepository,
            recallRepository,
            maintMileageRepository,
            maintMileageSummaryRepository,
            miscMaintCostRepository,
            providerClient,
            mapper,
            transactionTemplate,
            TEST_VEHICLE_DATA_EXECUTOR);

    var response = service.addVinToUser(user, new AddVinRequest("jtenu5jr6m5962554", 45000));

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

    VehicleOnboardingService service =
        new VehicleOnboardingService(
            vinRepository,
            vehicleTypeRepository,
            userVinRepository,
            recallRepository,
            maintMileageRepository,
            maintMileageSummaryRepository,
            miscMaintCostRepository,
            providerClient,
            new VehicleDataMapper(),
            transactionTemplate,
            TEST_VEHICLE_DATA_EXECUTOR);

    var response = service.addVinToUser(user, new AddVinRequest(" JTENU5JR6M5962554 ", 45000));

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

    VehicleOnboardingService service =
        new VehicleOnboardingService(
            vinRepository,
            vehicleTypeRepository,
            userVinRepository,
            recallRepository,
            maintMileageRepository,
            maintMileageSummaryRepository,
            miscMaintCostRepository,
            providerClient,
            new VehicleDataMapper(),
            transactionTemplate,
            TEST_VEHICLE_DATA_EXECUTOR);

    var response = service.addVinToUser(user, new AddVinRequest("jtenu5jr6m5962554", 45000));

    assertEquals("JTENU5JR6M5962554", response.vin());
    assertEquals(45000, response.currentMileage());
    assertTrue(response.createdVin());
    assertFalse(response.createdVehicleType());
    assertTrue(response.createdAssociation());
    assertEquals(PHOTO_1, response.selectedImageUrl());
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
  void shouldRejectMissingUserAndInvalidMileageBeforeCallingProvider() {
    VinRepositoryJPA vinRepository = mock(VinRepositoryJPA.class);
    VehicleTypeRepositoryJPA vehicleTypeRepository = mock(VehicleTypeRepositoryJPA.class);
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    RecallRepositoryJPA recallRepository = mock(RecallRepositoryJPA.class);
    MaintMileageRepositoryJPA maintMileageRepository = mock(MaintMileageRepositoryJPA.class);
    MaintMileageSummaryRepositoryJPA maintMileageSummaryRepository =
        mock(MaintMileageSummaryRepositoryJPA.class);
    MiscMaintCostRepositoryJPA miscMaintCostRepository = mock(MiscMaintCostRepositoryJPA.class);
    VehicleDataProviderClient providerClient = mock(VehicleDataProviderClient.class);
    VehicleOnboardingService service =
        new VehicleOnboardingService(
            vinRepository,
            vehicleTypeRepository,
            userVinRepository,
            recallRepository,
            maintMileageRepository,
            maintMileageSummaryRepository,
            miscMaintCostRepository,
            providerClient,
            new VehicleDataMapper(),
            transactionTemplate(),
            TEST_VEHICLE_DATA_EXECUTOR);

    assertEquals(
        "Authenticated user is required",
        assertThrows(
                IllegalArgumentException.class,
                () -> service.addVinToUser(null, new AddVinRequest("JTENU5JR6M5962554", 1)))
            .getMessage());
    assertEquals(
        "VIN must be 17 characters",
        assertThrows(
                IllegalArgumentException.class,
                () -> service.addVinToUser(user(), new AddVinRequest("too-short", 1)))
            .getMessage());
    assertEquals(
        "Current mileage cannot be negative",
        assertThrows(
                IllegalArgumentException.class,
                () -> service.addVinToUser(user(), new AddVinRequest("JTENU5JR6M5962554", -1)))
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

    VehicleOnboardingService service =
        new VehicleOnboardingService(
            vinRepository,
            vehicleTypeRepository,
            userVinRepository,
            recallRepository,
            maintMileageRepository,
            maintMileageSummaryRepository,
            miscMaintCostRepository,
            providerClient,
            new VehicleDataMapper(),
            transactionTemplate,
            TEST_VEHICLE_DATA_EXECUTOR);

    var response = service.addVinToUser(user, new AddVinRequest("JTENU5JR6M5962554", 45000));

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
    VehicleDataProviderClient providerClient = mock(VehicleDataProviderClient.class);
    TransactionTemplate transactionTemplate = transactionTemplate();
    User user = user();

    when(vinRepository.findById("JTENU5JR6M5962554")).thenReturn(Optional.empty());
    when(providerClient.decodeVin("JTENU5JR6M5962554")).thenReturn(invalidVinDecodeResponse());

    VehicleOnboardingService service =
        new VehicleOnboardingService(
            vinRepository,
            vehicleTypeRepository,
            userVinRepository,
            recallRepository,
            maintMileageRepository,
            maintMileageSummaryRepository,
            miscMaintCostRepository,
            providerClient,
            new VehicleDataMapper(),
            transactionTemplate,
            TEST_VEHICLE_DATA_EXECUTOR);

    assertThrows(
        VinNotFoundException.class,
        () -> service.addVinToUser(user, new AddVinRequest("JTENU5JR6M5962554", 45000)));
    verify(vehicleTypeRepository, never()).findByIdentity(any(), any(), any(), any(), any());
    verify(vinRepository, never()).save(any(Vin.class));
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
