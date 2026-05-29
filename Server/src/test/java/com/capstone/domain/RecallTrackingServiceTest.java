package com.capstone.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.capstone.data.CompletedRecallRepositoryJPA;
import com.capstone.data.RecallRepositoryJPA;
import com.capstone.data.UserVinRepositoryJPA;
import com.capstone.models.*;
import com.capstone.models.dto.CompleteRecallRequest;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RecallTrackingServiceTest {

  @Test
  void shouldUncompleteRecallForOwner() {
    CompletedRecallRepositoryJPA completedRecallRepository =
        mock(CompletedRecallRepositoryJPA.class);
    RecallTrackingService service =
        new RecallTrackingService(
            completedRecallRepository,
            mock(RecallRepositoryJPA.class),
            mock(UserVinRepositoryJPA.class));
    UserVin userVin = userVin();
    Recall recall = recall();
    CompletedRecall completedRecall =
        new CompletedRecall(
            55L, userVin, recall, LocalDate.of(2025, 3, 4), "Toyota dealer", 0.0, "Recall closed");

    when(completedRecallRepository.findById(55L)).thenReturn(Optional.of(completedRecall));

    assertTrue(service.uncompleteRecall(1L, 55L));
    verify(completedRecallRepository).delete(completedRecall);
  }

  @Test
  void shouldCreateCompletedRecallWithRequestFields() {
    CompletedRecallRepositoryJPA completedRecallRepository =
        mock(CompletedRecallRepositoryJPA.class);
    RecallRepositoryJPA recallRepository = mock(RecallRepositoryJPA.class);
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    RecallTrackingService service =
        new RecallTrackingService(completedRecallRepository, recallRepository, userVinRepository);
    UserVin userVin = userVin();
    Recall recall = recall();
    CompleteRecallRequest request =
        new CompleteRecallRequest(
            " jtenu5jr6m5962554 ",
            22L,
            LocalDate.of(2025, 4, 6),
            "Toyota dealer",
            0.0,
            "Airbag recall done");

    when(userVinRepository.findForUserVin(1L, "JTENU5JR6M5962554"))
        .thenReturn(Optional.of(userVin));
    when(recallRepository.findByRecallIdAndVehicleTypeId_VehicleTypeId(22L, 7L))
        .thenReturn(Optional.of(recall));
    when(completedRecallRepository.findByUserVinAndRecall(userVin, recall))
        .thenReturn(Optional.empty());
    when(completedRecallRepository.save(any(CompletedRecall.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response = service.completeRecall(1L, request);

    ArgumentCaptor<CompletedRecall> captor = ArgumentCaptor.forClass(CompletedRecall.class);
    verify(completedRecallRepository).save(captor.capture());
    CompletedRecall saved = captor.getValue();
    assertEquals(userVin, saved.getUserVin());
    assertEquals(recall, saved.getRecall());
    assertEquals(LocalDate.of(2025, 4, 6), saved.getCompletedDate());
    assertEquals("Toyota dealer", saved.getRepairShop());
    assertEquals(0.0, saved.getCost());
    assertEquals("Airbag recall done", saved.getNotes());
    assertEquals("JTENU5JR6M5962554", response.vin());
    assertEquals(22L, response.recallId());
  }

  @Test
  void shouldReturnExistingCompletedRecallWithoutSavingDuplicate() {
    CompletedRecallRepositoryJPA completedRecallRepository =
        mock(CompletedRecallRepositoryJPA.class);
    RecallRepositoryJPA recallRepository = mock(RecallRepositoryJPA.class);
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    RecallTrackingService service =
        new RecallTrackingService(completedRecallRepository, recallRepository, userVinRepository);
    UserVin userVin = userVin();
    Recall recall = recall();
    CompletedRecall existing =
        new CompletedRecall(
            55L, userVin, recall, LocalDate.of(2025, 3, 4), "Toyota dealer", 0.0, "Already closed");
    CompleteRecallRequest request =
        new CompleteRecallRequest(
            "JTENU5JR6M5962554",
            22L,
            LocalDate.of(2025, 4, 6),
            "Other shop",
            25.0,
            "Duplicate request");

    when(userVinRepository.findForUserVin(1L, "JTENU5JR6M5962554"))
        .thenReturn(Optional.of(userVin));
    when(recallRepository.findByRecallIdAndVehicleTypeId_VehicleTypeId(22L, 7L))
        .thenReturn(Optional.of(recall));
    when(completedRecallRepository.findByUserVinAndRecall(userVin, recall))
        .thenReturn(Optional.of(existing));

    var response = service.completeRecall(1L, request);

    assertEquals(55L, response.completedRecallId());
    assertEquals(LocalDate.of(2025, 3, 4), response.completedDate());
    assertEquals("Toyota dealer", response.repairShop());
    assertEquals("Already closed", response.notes());
    verify(completedRecallRepository, never()).save(any(CompletedRecall.class));
  }

  @Test
  void shouldRejectRecallForDifferentVehicleType() {
    RecallRepositoryJPA recallRepository = mock(RecallRepositoryJPA.class);
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    RecallTrackingService service =
        new RecallTrackingService(
            mock(CompletedRecallRepositoryJPA.class), recallRepository, userVinRepository);
    CompleteRecallRequest request =
        new CompleteRecallRequest(
            "JTENU5JR6M5962554", 22L, LocalDate.of(2025, 4, 6), "Toyota dealer", 0.0, null);

    when(userVinRepository.findForUserVin(1L, "JTENU5JR6M5962554"))
        .thenReturn(Optional.of(userVin()));
    when(recallRepository.findByRecallIdAndVehicleTypeId_VehicleTypeId(22L, 7L))
        .thenReturn(Optional.empty());

    assertThrows(RecallNotFoundException.class, () -> service.completeRecall(1L, request));
  }

  @Test
  void shouldRejectMissingRecall() {
    RecallTrackingService service =
        new RecallTrackingService(
            mock(CompletedRecallRepositoryJPA.class),
            mock(RecallRepositoryJPA.class),
            mock(UserVinRepositoryJPA.class));

    assertEquals(
        "Recall is required",
        assertThrows(IllegalArgumentException.class, () -> service.completeRecall(1L, null))
            .getMessage());
    assertEquals(
        "Recall is required",
        assertThrows(
                IllegalArgumentException.class,
                () ->
                    service.completeRecall(
                        1L,
                        new CompleteRecallRequest(
                            "JTENU5JR6M5962554", null, null, null, null, null)))
            .getMessage());
  }

  private UserVin userVin() {
    User user = new User();
    user.setUserId(1L);
    return new UserVin(user, new Vin("JTENU5JR6M5962554", vehicleType()), 45000);
  }

  private Recall recall() {
    return new Recall(
        22L,
        vehicleType(),
        "22V480000",
        "",
        LocalDate.of(2022, 6, 7),
        "EQUIPMENT:OTHER:LABELS",
        "Summary",
        "Consequence",
        "Remedy",
        "Notes",
        "Southeast Toyota Distributors, LLC");
  }

  private VehicleType vehicleType() {
    VehicleType vehicleType = new VehicleType("Toyota", "4RUNNER", "SRS Prem", "2021");
    vehicleType.setVehicleTypeId(7L);
    return vehicleType;
  }
}
