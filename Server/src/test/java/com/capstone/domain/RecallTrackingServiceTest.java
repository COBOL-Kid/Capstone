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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RecallTrackingServiceTest {

  @Test
  void shouldFindUncompletedRecallsForNormalizedVin() {
    CompletedRecallRepositoryJPA completedRecallRepository =
        mock(CompletedRecallRepositoryJPA.class);
    RecallRepositoryJPA recallRepository = mock(RecallRepositoryJPA.class);
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    RecallTrackingService service =
        new RecallTrackingService(completedRecallRepository, recallRepository, userVinRepository);
    Recall recall = recall();

    when(recallRepository.findUncompletedRecalls(1L, "JTENU5JR6M5962554"))
        .thenReturn(List.of(recall));

    var responses = service.findUncompletedRecalls(user(), " jtenu5jr6m5962554 ");

    assertEquals(1, responses.size());
    assertEquals(22L, responses.getFirst().recallId());
    assertEquals("JTENU5JR6M5962554", responses.getFirst().vin());
    assertEquals("22V480000", responses.getFirst().nhtsaCampaignNumber());
    assertEquals(LocalDate.of(2022, 6, 7), responses.getFirst().reportReceivedDate());
    assertEquals("EQUIPMENT:OTHER:LABELS", responses.getFirst().component());
    assertEquals("Summary", responses.getFirst().summary());
    assertEquals("Consequence", responses.getFirst().consequence());
    assertEquals("Remedy", responses.getFirst().remedy());
  }

  @Test
  void shouldFindCompletedRecallsForNormalizedVin() {
    CompletedRecallRepositoryJPA completedRecallRepository =
        mock(CompletedRecallRepositoryJPA.class);
    RecallRepositoryJPA recallRepository = mock(RecallRepositoryJPA.class);
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    RecallTrackingService service =
        new RecallTrackingService(completedRecallRepository, recallRepository, userVinRepository);
    UserVin userVin = userVin();
    Recall recall = recall();
    CompletedRecall completedRecall =
        new CompletedRecall(
            55L, userVin, recall, LocalDate.of(2025, 3, 4), "Toyota dealer", 0.0, "Recall closed");

    when(completedRecallRepository.findAllForUserVin(1L, "JTENU5JR6M5962554"))
        .thenReturn(List.of(completedRecall));

    var responses = service.findCompletedRecalls(user(), " jtenu5jr6m5962554 ");

    assertEquals(1, responses.size());
    assertEquals(55L, responses.getFirst().completedRecallId());
    assertEquals("JTENU5JR6M5962554", responses.getFirst().vin());
    assertEquals(22L, responses.getFirst().recallId());
    assertEquals(LocalDate.of(2025, 3, 4), responses.getFirst().completedDate());
    assertEquals("Toyota dealer", responses.getFirst().repairShop());
    assertEquals(0.0, responses.getFirst().cost());
    assertEquals("Recall closed", responses.getFirst().notes());
    assertEquals("22V480000", responses.getFirst().nhtsaCampaignNumber());
    assertEquals("Summary", responses.getFirst().summary());
  }

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

    assertTrue(service.uncompleteRecall(user(), 55L));
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

    when(userVinRepository.findByUserUserIdAndVinVin(1L, "JTENU5JR6M5962554"))
        .thenReturn(Optional.of(userVin));
    when(recallRepository.findById(22L)).thenReturn(Optional.of(recall));
    when(completedRecallRepository.findByUserVinAndRecall(userVin, recall))
        .thenReturn(Optional.empty());
    when(completedRecallRepository.save(any(CompletedRecall.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response = service.completeRecall(user(), request);

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

    when(userVinRepository.findByUserUserIdAndVinVin(1L, "JTENU5JR6M5962554"))
        .thenReturn(Optional.of(userVin));
    when(recallRepository.findById(22L)).thenReturn(Optional.of(recall));
    when(completedRecallRepository.findByUserVinAndRecall(userVin, recall))
        .thenReturn(Optional.of(existing));

    var response = service.completeRecall(user(), request);

    assertEquals(55L, response.completedRecallId());
    assertEquals(LocalDate.of(2025, 3, 4), response.completedDate());
    assertEquals("Toyota dealer", response.repairShop());
    assertEquals("Already closed", response.notes());
    verify(completedRecallRepository, never()).save(any(CompletedRecall.class));
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
        assertThrows(IllegalArgumentException.class, () -> service.completeRecall(user(), null))
            .getMessage());
    assertEquals(
        "Recall is required",
        assertThrows(
                IllegalArgumentException.class,
                () ->
                    service.completeRecall(
                        user(),
                        new CompleteRecallRequest(
                            "JTENU5JR6M5962554", null, null, null, null, null)))
            .getMessage());
  }

  private User user() {
    User user = new User();
    user.setUserId(1L);
    user.setUserEmail("driver@example.com");
    return user;
  }

  private UserVin userVin() {
    return new UserVin(user(), new Vin("JTENU5JR6M5962554", 45000, vehicleType()), 45000);
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
