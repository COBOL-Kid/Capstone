package com.capstone.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.capstone.authentication.AuthenticatedUser;
import com.capstone.domain.RecallNotFoundException;
import com.capstone.domain.RecallTrackingService;
import com.capstone.models.Role;
import com.capstone.models.dto.CompleteRecallRequest;
import com.capstone.models.dto.CompletedRecallResponse;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class RecallControllerTest {

  @Test
  void shouldRequireAuthentication() {
    RecallController controller = new RecallController(mock(RecallTrackingService.class));

    assertEquals(
        HttpStatus.UNAUTHORIZED,
        controller
            .completeRecall(
                null, new CompleteRecallRequest("JTENU5JR6M5962554", 22L, null, null, null, null))
            .getStatusCode());
    assertEquals(HttpStatus.UNAUTHORIZED, controller.uncompleteRecall(null, 55L).getStatusCode());
  }

  @Test
  void shouldUncompleteRecall() {
    RecallTrackingService service = mock(RecallTrackingService.class);
    RecallController controller = new RecallController(service);
    AuthenticatedUser user = user();

    when(service.uncompleteRecall(1L, 55L)).thenReturn(true);

    assertEquals(HttpStatus.NO_CONTENT, controller.uncompleteRecall(user, 55L).getStatusCode());
  }

  @Test
  void shouldCreateCompletedRecall() {
    RecallTrackingService service = mock(RecallTrackingService.class);
    RecallController controller = new RecallController(service);
    AuthenticatedUser user = user();
    CompleteRecallRequest request =
        new CompleteRecallRequest(
            "JTENU5JR6M5962554",
            22L,
            LocalDate.of(2025, 4, 6),
            "Toyota dealer",
            0.0,
            "Airbag recall done");
    CompletedRecallResponse completed = completedRecall();

    when(service.completeRecall(1L, request)).thenReturn(completed);

    var response = controller.completeRecall(user, request);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(completed, response.getBody());
  }

  @Test
  void shouldReturnNotFoundWhenUncompletingMissingRecall() {
    RecallTrackingService service = mock(RecallTrackingService.class);
    RecallController controller = new RecallController(service);
    AuthenticatedUser user = user();

    when(service.uncompleteRecall(1L, 55L)).thenThrow(new RecallNotFoundException());

    assertEquals(HttpStatus.NOT_FOUND, controller.uncompleteRecall(user, 55L).getStatusCode());
  }

  private AuthenticatedUser user() {
    return new AuthenticatedUser(1L, "driver@example.com", Role.USER);
  }

  private CompletedRecallResponse completedRecall() {
    return new CompletedRecallResponse(
        55L,
        "JTENU5JR6M5962554",
        22L,
        LocalDate.of(2025, 4, 6),
        "Toyota dealer",
        0.0,
        "Airbag recall done",
        "22V480000",
        LocalDate.of(2022, 6, 7),
        "EQUIPMENT:OTHER:LABELS",
        "Summary",
        "Consequence",
        "Remedy");
  }
}
