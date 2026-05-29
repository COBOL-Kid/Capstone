package com.capstone.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.capstone.data.CompletedMaintenanceRepositoryJPA;
import com.capstone.data.MaintMileageRepositoryJPA;
import com.capstone.data.UserVinRepositoryJPA;
import com.capstone.models.*;
import com.capstone.models.dto.CompleteMaintenanceRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MaintenanceTrackingServiceTest {

  @Test
  void shouldUncompleteMaintenanceForOwner() {
    CompletedMaintenanceRepositoryJPA completedMaintenanceRepository =
        mock(CompletedMaintenanceRepositoryJPA.class);
    MaintenanceTrackingService service =
        new MaintenanceTrackingService(
            completedMaintenanceRepository,
            mock(MaintMileageRepositoryJPA.class),
            mock(UserVinRepositoryJPA.class));
    UserVin userVin = userVin(32000);
    MaintMileage maintMileage = maintMileage();
    CompletedMaintenance completedMaintenance =
        new CompletedMaintenance(
            99L, userVin, maintMileage, LocalDate.of(2025, 2, 3), 31000, 89.99, "Changed oil");

    when(completedMaintenanceRepository.findById(99L))
        .thenReturn(Optional.of(completedMaintenance));

    assertTrue(service.uncompleteMaintenance(1L, 99L));
    verify(completedMaintenanceRepository).delete(completedMaintenance);
  }

  @Test
  void shouldRejectUncompleteMaintenanceForOtherUser() {
    CompletedMaintenanceRepositoryJPA completedMaintenanceRepository =
        mock(CompletedMaintenanceRepositoryJPA.class);
    MaintenanceTrackingService service =
        new MaintenanceTrackingService(
            completedMaintenanceRepository,
            mock(MaintMileageRepositoryJPA.class),
            mock(UserVinRepositoryJPA.class));
    User otherUser = new User();
    otherUser.setUserId(2L);
    UserVin userVin = new UserVin(otherUser, new Vin("JTENU5JR6M5962554", vehicleType()), 32000);
    CompletedMaintenance completedMaintenance =
        new CompletedMaintenance(
            99L, userVin, maintMileage(), LocalDate.of(2025, 2, 3), 31000, 89.99, "Changed oil");

    when(completedMaintenanceRepository.findById(99L))
        .thenReturn(Optional.of(completedMaintenance));

    assertThrows(
        MaintenanceItemNotFoundException.class, () -> service.uncompleteMaintenance(1L, 99L));
    verify(completedMaintenanceRepository, never()).delete(any(CompletedMaintenance.class));
  }

  @Test
  void shouldCreateCompletedMaintenanceWithRequestFields() {
    CompletedMaintenanceRepositoryJPA completedMaintenanceRepository =
        mock(CompletedMaintenanceRepositoryJPA.class);
    MaintMileageRepositoryJPA maintMileageRepository = mock(MaintMileageRepositoryJPA.class);
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    MaintenanceTrackingService service =
        new MaintenanceTrackingService(
            completedMaintenanceRepository, maintMileageRepository, userVinRepository);
    UserVin userVin = userVin(45000);
    MaintMileage maintMileage = maintMileage();
    CompleteMaintenanceRequest request =
        new CompleteMaintenanceRequest(
            " jtenu5jr6m5962554 ", 11L, LocalDate.of(2025, 4, 5), 45100, 120.50, "Dealer service");

    when(userVinRepository.findForUserVin(1L, "JTENU5JR6M5962554"))
        .thenReturn(Optional.of(userVin));
    when(maintMileageRepository.findByMaintMileageIdAndVehicleTypeId_VehicleTypeId(11L, 7L))
        .thenReturn(Optional.of(maintMileage));
    when(completedMaintenanceRepository.findByUserVinAndMaintMileage(userVin, maintMileage))
        .thenReturn(Optional.empty());
    when(completedMaintenanceRepository.save(any(CompletedMaintenance.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response = service.completeMaintenance(1L, request);

    ArgumentCaptor<CompletedMaintenance> captor =
        ArgumentCaptor.forClass(CompletedMaintenance.class);
    verify(completedMaintenanceRepository).save(captor.capture());
    CompletedMaintenance saved = captor.getValue();
    assertEquals(userVin, saved.getUserVin());
    assertEquals(maintMileage, saved.getMaintMileage());
    assertEquals(LocalDate.of(2025, 4, 5), saved.getCompletedDate());
    assertEquals(45100, saved.getMileageCompleted());
    assertEquals(120.50, saved.getCost());
    assertEquals("Dealer service", saved.getNotes());
    assertEquals("JTENU5JR6M5962554", response.vin());
    assertEquals(11L, response.maintMileageId());
  }

  @Test
  void shouldReturnExistingCompletedMaintenanceWithoutSavingDuplicate() {
    CompletedMaintenanceRepositoryJPA completedMaintenanceRepository =
        mock(CompletedMaintenanceRepositoryJPA.class);
    MaintMileageRepositoryJPA maintMileageRepository = mock(MaintMileageRepositoryJPA.class);
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    MaintenanceTrackingService service =
        new MaintenanceTrackingService(
            completedMaintenanceRepository, maintMileageRepository, userVinRepository);
    UserVin userVin = userVin(45000);
    MaintMileage maintMileage = maintMileage();
    CompletedMaintenance existing =
        new CompletedMaintenance(
            99L, userVin, maintMileage, LocalDate.of(2025, 1, 2), 44000, null, "Already done");
    CompleteMaintenanceRequest request =
        new CompleteMaintenanceRequest(
            "JTENU5JR6M5962554", 11L, LocalDate.of(2025, 4, 5), 45100, 120.50, "Duplicate request");

    when(userVinRepository.findForUserVin(1L, "JTENU5JR6M5962554"))
        .thenReturn(Optional.of(userVin));
    when(maintMileageRepository.findByMaintMileageIdAndVehicleTypeId_VehicleTypeId(11L, 7L))
        .thenReturn(Optional.of(maintMileage));
    when(completedMaintenanceRepository.findByUserVinAndMaintMileage(userVin, maintMileage))
        .thenReturn(Optional.of(existing));

    var response = service.completeMaintenance(1L, request);

    assertEquals(99L, response.completedMaintenanceId());
    assertEquals(LocalDate.of(2025, 1, 2), response.completedDate());
    assertEquals(44000, response.mileageCompleted());
    assertEquals("Already done", response.notes());
    verify(completedMaintenanceRepository, never()).save(any(CompletedMaintenance.class));
  }

  @Test
  void shouldRejectMaintenanceItemForDifferentVehicleType() {
    MaintMileageRepositoryJPA maintMileageRepository = mock(MaintMileageRepositoryJPA.class);
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    MaintenanceTrackingService service =
        new MaintenanceTrackingService(
            mock(CompletedMaintenanceRepositoryJPA.class),
            maintMileageRepository,
            userVinRepository);
    UserVin userVin = userVin(45000);
    CompleteMaintenanceRequest request =
        new CompleteMaintenanceRequest(
            "JTENU5JR6M5962554", 11L, LocalDate.of(2025, 4, 5), 45100, 120.50, null);

    when(userVinRepository.findForUserVin(1L, "JTENU5JR6M5962554"))
        .thenReturn(Optional.of(userVin));
    when(maintMileageRepository.findByMaintMileageIdAndVehicleTypeId_VehicleTypeId(11L, 7L))
        .thenReturn(Optional.empty());

    assertThrows(
        MaintenanceItemNotFoundException.class, () -> service.completeMaintenance(1L, request));
  }

  @Test
  void shouldRejectMissingMaintenanceItem() {
    MaintenanceTrackingService service =
        new MaintenanceTrackingService(
            mock(CompletedMaintenanceRepositoryJPA.class),
            mock(MaintMileageRepositoryJPA.class),
            mock(UserVinRepositoryJPA.class));

    assertEquals(
        "Maintenance item is required",
        assertThrows(IllegalArgumentException.class, () -> service.completeMaintenance(1L, null))
            .getMessage());
    assertEquals(
        "Maintenance item is required",
        assertThrows(
                IllegalArgumentException.class,
                () ->
                    service.completeMaintenance(
                        1L,
                        new CompleteMaintenanceRequest(
                            "JTENU5JR6M5962554", null, null, 1, null, null)))
            .getMessage());
  }

  private UserVin userVin(int currentMileage) {
    User user = new User();
    user.setUserId(1L);
    return new UserVin(user, new Vin("JTENU5JR6M5962554", vehicleType()), currentMileage);
  }

  private MaintMileage maintMileage() {
    MaintMileage maintMileage = new MaintMileage(11L, vehicleType(), 50000, "Replace engine oil");
    maintMileage.setLaborLine(
        new MaintLaborLine(
            maintMileage,
            new BigDecimal("0.50"),
            new BigDecimal("85.00"),
            new BigDecimal("42.50"),
            "USD"));
    maintMileage
        .getPartLines()
        .add(new MaintPartLine(maintMileage, "Engine oil", new BigDecimal("35.00"), "USD"));
    return maintMileage;
  }

  private VehicleType vehicleType() {
    VehicleType vehicleType = new VehicleType("Toyota", "4RUNNER", "SRS Prem", "2021");
    vehicleType.setVehicleTypeId(7L);
    return vehicleType;
  }
}
