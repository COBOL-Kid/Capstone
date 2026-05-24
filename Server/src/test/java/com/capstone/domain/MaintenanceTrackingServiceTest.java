package com.capstone.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.capstone.data.CompletedMaintenanceRepositoryJPA;
import com.capstone.data.MaintCostRepositoryJPA;
import com.capstone.data.MaintMileageRepositoryJPA;
import com.capstone.data.UserVinRepositoryJPA;
import com.capstone.models.*;
import com.capstone.models.dto.CompleteMaintenanceRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MaintenanceTrackingServiceTest {

  @Test
  void shouldFindCompletedMaintenanceForNormalizedVin() {
    CompletedMaintenanceRepositoryJPA completedMaintenanceRepository =
        mock(CompletedMaintenanceRepositoryJPA.class);
    MaintMileageRepositoryJPA maintMileageRepository = mock(MaintMileageRepositoryJPA.class);
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    MaintCostRepositoryJPA maintCostRepository = mock(MaintCostRepositoryJPA.class);
    MaintenanceTrackingService service =
        new MaintenanceTrackingService(
            completedMaintenanceRepository,
            maintMileageRepository,
            userVinRepository,
            maintCostRepository);
    UserVin userVin = userVin(32000);
    MaintMileage maintMileage = maintMileage();
    CompletedMaintenance completedMaintenance =
        new CompletedMaintenance(
            99L, userVin, maintMileage, LocalDate.of(2025, 2, 3), 31000, 89.99, "Changed oil");

    when(completedMaintenanceRepository.findAllForUserVin(1L, "JTENU5JR6M5962554"))
        .thenReturn(List.of(completedMaintenance));

    var responses = service.findCompletedMaintenance(user(), " jtenu5jr6m5962554 ");

    assertEquals(1, responses.size());
    assertEquals(99L, responses.getFirst().completedMaintenanceId());
    assertEquals("JTENU5JR6M5962554", responses.getFirst().vin());
    assertEquals(11L, responses.getFirst().maintMileageId());
    assertEquals(LocalDate.of(2025, 2, 3), responses.getFirst().completedDate());
    assertEquals(31000, responses.getFirst().mileageCompleted());
    assertEquals(89.99, responses.getFirst().cost());
    assertEquals("Changed oil", responses.getFirst().notes());
    assertEquals("Replace engine oil", responses.getFirst().maintDesc());
    assertEquals(50000, responses.getFirst().mileageDue());
  }

  @Test
  void shouldUncompleteMaintenanceForOwner() {
    CompletedMaintenanceRepositoryJPA completedMaintenanceRepository =
        mock(CompletedMaintenanceRepositoryJPA.class);
    MaintenanceTrackingService service =
        new MaintenanceTrackingService(
            completedMaintenanceRepository,
            mock(MaintMileageRepositoryJPA.class),
            mock(UserVinRepositoryJPA.class),
            mock(MaintCostRepositoryJPA.class));
    UserVin userVin = userVin(32000);
    MaintMileage maintMileage = maintMileage();
    CompletedMaintenance completedMaintenance =
        new CompletedMaintenance(
            99L, userVin, maintMileage, LocalDate.of(2025, 2, 3), 31000, 89.99, "Changed oil");

    when(completedMaintenanceRepository.findById(99L))
        .thenReturn(Optional.of(completedMaintenance));

    assertTrue(service.uncompleteMaintenance(user(), 99L));
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
            mock(UserVinRepositoryJPA.class),
            mock(MaintCostRepositoryJPA.class));
    User otherUser = new User();
    otherUser.setUserId(2L);
    UserVin userVin =
        new UserVin(otherUser, new Vin("JTENU5JR6M5962554", 32000, vehicleType()), 32000);
    CompletedMaintenance completedMaintenance =
        new CompletedMaintenance(
            99L, userVin, maintMileage(), LocalDate.of(2025, 2, 3), 31000, 89.99, "Changed oil");

    when(completedMaintenanceRepository.findById(99L))
        .thenReturn(Optional.of(completedMaintenance));

    assertThrows(
        MaintenanceItemNotFoundException.class, () -> service.uncompleteMaintenance(user(), 99L));
    verify(completedMaintenanceRepository, never()).delete(any(CompletedMaintenance.class));
  }

  @Test
  void shouldFindUpcomingMaintenance() {
    CompletedMaintenanceRepositoryJPA completedMaintenanceRepository =
        mock(CompletedMaintenanceRepositoryJPA.class);
    MaintMileageRepositoryJPA maintMileageRepository = mock(MaintMileageRepositoryJPA.class);
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    MaintCostRepositoryJPA maintCostRepository = mock(MaintCostRepositoryJPA.class);
    MaintenanceTrackingService service =
        new MaintenanceTrackingService(
            completedMaintenanceRepository,
            maintMileageRepository,
            userVinRepository,
            maintCostRepository);
    UserVin userVin = userVin(32000);
    MaintMileage maintMileage = maintMileage();

    when(userVinRepository.findForUserVin(1L, "JTENU5JR6M5962554"))
        .thenReturn(Optional.of(userVin));
    when(maintMileageRepository.findUpcomingAndPastDue(7L, 42000, 1L, "JTENU5JR6M5962554"))
        .thenReturn(List.of(maintMileage));

    var responses = service.findUpcomingMaintenance(user(), " jtenu5jr6m5962554 ");

    assertEquals(1, responses.size());
    assertEquals(11L, responses.getFirst().maintMileageId());
    assertEquals("JTENU5JR6M5962554", responses.getFirst().vin());
    assertEquals(50000, responses.getFirst().mileageDue());
    assertEquals("Replace engine oil", responses.getFirst().maintDesc());
  }

  @Test
  void shouldCreateCompletedMaintenanceWithRequestFields() {
    CompletedMaintenanceRepositoryJPA completedMaintenanceRepository =
        mock(CompletedMaintenanceRepositoryJPA.class);
    MaintMileageRepositoryJPA maintMileageRepository = mock(MaintMileageRepositoryJPA.class);
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    MaintCostRepositoryJPA maintCostRepository = mock(MaintCostRepositoryJPA.class);
    MaintenanceTrackingService service =
        new MaintenanceTrackingService(
            completedMaintenanceRepository,
            maintMileageRepository,
            userVinRepository,
            maintCostRepository);
    UserVin userVin = userVin(45000);
    MaintMileage maintMileage = maintMileage();
    CompleteMaintenanceRequest request =
        new CompleteMaintenanceRequest(
            " jtenu5jr6m5962554 ", 11L, LocalDate.of(2025, 4, 5), 45100, 120.50, "Dealer service");

    when(userVinRepository.findByUserUserIdAndVinVin(1L, "JTENU5JR6M5962554"))
        .thenReturn(Optional.of(userVin));
    when(maintMileageRepository.findById(11L)).thenReturn(Optional.of(maintMileage));
    when(completedMaintenanceRepository.findByUserVinAndMaintMileage(userVin, maintMileage))
        .thenReturn(Optional.empty());
    when(completedMaintenanceRepository.save(any(CompletedMaintenance.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response = service.completeMaintenance(user(), request);

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
    MaintCostRepositoryJPA maintCostRepository = mock(MaintCostRepositoryJPA.class);
    MaintenanceTrackingService service =
        new MaintenanceTrackingService(
            completedMaintenanceRepository,
            maintMileageRepository,
            userVinRepository,
            maintCostRepository);
    UserVin userVin = userVin(45000);
    MaintMileage maintMileage = maintMileage();
    CompletedMaintenance existing =
        new CompletedMaintenance(
            99L, userVin, maintMileage, LocalDate.of(2025, 1, 2), 44000, null, "Already done");
    CompleteMaintenanceRequest request =
        new CompleteMaintenanceRequest(
            "JTENU5JR6M5962554", 11L, LocalDate.of(2025, 4, 5), 45100, 120.50, "Duplicate request");

    when(userVinRepository.findByUserUserIdAndVinVin(1L, "JTENU5JR6M5962554"))
        .thenReturn(Optional.of(userVin));
    when(maintMileageRepository.findById(11L)).thenReturn(Optional.of(maintMileage));
    when(completedMaintenanceRepository.findByUserVinAndMaintMileage(userVin, maintMileage))
        .thenReturn(Optional.of(existing));

    var response = service.completeMaintenance(user(), request);

    assertEquals(99L, response.completedMaintenanceId());
    assertEquals(LocalDate.of(2025, 1, 2), response.completedDate());
    assertEquals(44000, response.mileageCompleted());
    assertEquals("Already done", response.notes());
    verify(completedMaintenanceRepository, never()).save(any(CompletedMaintenance.class));
  }

  @Test
  void shouldRejectMissingMaintenanceItem() {
    MaintenanceTrackingService service =
        new MaintenanceTrackingService(
            mock(CompletedMaintenanceRepositoryJPA.class), mock(MaintMileageRepositoryJPA.class),
            mock(UserVinRepositoryJPA.class), mock(MaintCostRepositoryJPA.class));

    assertEquals(
        "Maintenance item is required",
        assertThrows(
                IllegalArgumentException.class, () -> service.completeMaintenance(user(), null))
            .getMessage());
    assertEquals(
        "Maintenance item is required",
        assertThrows(
                IllegalArgumentException.class,
                () ->
                    service.completeMaintenance(
                        user(),
                        new CompleteMaintenanceRequest(
                            "JTENU5JR6M5962554", null, null, 1, null, null)))
            .getMessage());
  }

  @Test
  void shouldFindMaintenanceCostsForNormalizedVin() {
    CompletedMaintenanceRepositoryJPA completedMaintenanceRepository =
        mock(CompletedMaintenanceRepositoryJPA.class);
    MaintMileageRepositoryJPA maintMileageRepository = mock(MaintMileageRepositoryJPA.class);
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    MaintCostRepositoryJPA maintCostRepository = mock(MaintCostRepositoryJPA.class);
    MaintenanceTrackingService service =
        new MaintenanceTrackingService(
            completedMaintenanceRepository,
            maintMileageRepository,
            userVinRepository,
            maintCostRepository);
    UserVin userVin = userVin(32000);

    MaintCost cost =
        new MaintCost(
            10L,
            vehicleType(),
            "Change Battery",
            "Replaces the battery",
            100,
            150,
            80,
            200,
            250,
            150);

    when(userVinRepository.findForUserVin(1L, "JTENU5JR6M5962554"))
        .thenReturn(Optional.of(userVin));
    when(maintCostRepository.findByVehicleTypeId_VehicleTypeId(7L)).thenReturn(List.of(cost));

    var responses = service.findMaintenanceCosts(user(), " jtenu5jr6m5962554 ");

    assertEquals(1, responses.size());
    assertEquals(10L, responses.getFirst().maintCostId());
    assertEquals("Change Battery", responses.getFirst().maintTitle());
    assertEquals("Replaces the battery", responses.getFirst().maintDesc());
    assertEquals(100, responses.getFirst().independentAvg());
    assertEquals(200, responses.getFirst().dealerAvg());
  }

  @Test
  void shouldThrowVinNotAssociatedExceptionWhenFindingMaintenanceCosts() {
    CompletedMaintenanceRepositoryJPA completedMaintenanceRepository =
        mock(CompletedMaintenanceRepositoryJPA.class);
    MaintMileageRepositoryJPA maintMileageRepository = mock(MaintMileageRepositoryJPA.class);
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    MaintCostRepositoryJPA maintCostRepository = mock(MaintCostRepositoryJPA.class);
    MaintenanceTrackingService service =
        new MaintenanceTrackingService(
            completedMaintenanceRepository,
            maintMileageRepository,
            userVinRepository,
            maintCostRepository);

    when(userVinRepository.findForUserVin(1L, "JTENU5JR6M5962554")).thenReturn(Optional.empty());

    assertThrows(
        VinNotAssociatedException.class,
        () -> service.findMaintenanceCosts(user(), "JTENU5JR6M5962554"));
  }

  private User user() {
    User user = new User();
    user.setUserId(1L);
    user.setUserEmail("driver@example.com");
    return user;
  }

  private UserVin userVin(int currentMileage) {
    return new UserVin(
        user(), new Vin("JTENU5JR6M5962554", currentMileage, vehicleType()), currentMileage);
  }

  private MaintMileage maintMileage() {
    return new MaintMileage(11L, vehicleType(), 50000, "Replace engine oil");
  }

  private VehicleType vehicleType() {
    VehicleType vehicleType = new VehicleType("Toyota", "4RUNNER", "SRS Prem", "2021");
    vehicleType.setVehicleTypeId(7L);
    return vehicleType;
  }
}
