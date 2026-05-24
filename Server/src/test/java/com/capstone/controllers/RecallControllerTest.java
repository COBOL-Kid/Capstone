package com.capstone.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.capstone.domain.RecallNotFoundException;
import com.capstone.domain.RecallTrackingService;
import com.capstone.models.User;
import com.capstone.models.dto.CompleteRecallRequest;
import com.capstone.models.dto.CompletedRecallResponse;
import com.capstone.models.dto.RecallResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class RecallControllerTest {

  @Test
  void shouldRequireAuthentication() {
    RecallController controller = new RecallController(mock(RecallTrackingService.class));

    assertEquals(
        HttpStatus.UNAUTHORIZED,
        controller.getCompletedRecalls(null, "JTENU5JR6M5962554").getStatusCode());
    assertEquals(
        HttpStatus.UNAUTHORIZED,
        controller.getUncompletedRecalls(null, "JTENU5JR6M5962554").getStatusCode());
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
    User user = user();

    when(service.uncompleteRecall(user, 55L)).thenReturn(true);

    assertEquals(HttpStatus.NO_CONTENT, controller.uncompleteRecall(user, 55L).getStatusCode());
  }

  @Test
  void shouldReturnNoContentWhenNoCompletedRecallsExist() {
    RecallTrackingService service = mock(RecallTrackingService.class);
    RecallController controller = new RecallController(service);
    User user = user();

    when(service.findCompletedRecalls(user, "JTENU5JR6M5962554")).thenReturn(List.of());

    assertEquals(
        HttpStatus.NO_CONTENT,
        controller.getCompletedRecalls(user, "JTENU5JR6M5962554").getStatusCode());
  }

  @Test
  void shouldReturnNoContentWhenNoUncompletedRecallsExist() {
    RecallTrackingService service = mock(RecallTrackingService.class);
    RecallController controller = new RecallController(service);
    User user = user();

    when(service.findUncompletedRecalls(user, "JTENU5JR6M5962554")).thenReturn(List.of());

    assertEquals(
        HttpStatus.NO_CONTENT,
        controller.getUncompletedRecalls(user, "JTENU5JR6M5962554").getStatusCode());
  }

  @Test
  void shouldReturnUncompletedRecalls() {
    RecallTrackingService service = mock(RecallTrackingService.class);
    RecallController controller = new RecallController(service);
    User user = user();
    RecallResponse uncompleted = recallResponse();

    when(service.findUncompletedRecalls(user, "JTENU5JR6M5962554"))
        .thenReturn(List.of(uncompleted));

    var response = controller.getUncompletedRecalls(user, "JTENU5JR6M5962554");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(List.of(uncompleted), response.getBody());
  }

  @Test
  void shouldReturnCompletedRecalls() {
    RecallTrackingService service = mock(RecallTrackingService.class);
    RecallController controller = new RecallController(service);
    User user = user();
    CompletedRecallResponse completed = completedRecall();

    when(service.findCompletedRecalls(user, "JTENU5JR6M5962554")).thenReturn(List.of(completed));

    var response = controller.getCompletedRecalls(user, "JTENU5JR6M5962554");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(List.of(completed), response.getBody());
  }

  @Test
  void shouldCreateCompletedRecall() {
    RecallTrackingService service = mock(RecallTrackingService.class);
    RecallController controller = new RecallController(service);
    User user = user();
    CompleteRecallRequest request =
        new CompleteRecallRequest(
            "JTENU5JR6M5962554",
            22L,
            LocalDate.of(2025, 4, 6),
            "Toyota dealer",
            0.0,
            "Airbag recall done");
    CompletedRecallResponse completed = completedRecall();

    when(service.completeRecall(user, request)).thenReturn(completed);

    var response = controller.completeRecall(user, request);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(completed, response.getBody());
  }

  private User user() {
    User user = new User();
    user.setUserId(1L);
    user.setUserEmail("driver@example.com");
    return user;
  }

  @Test
  void shouldReturnNotFoundWhenUncompletingMissingRecall() {
    RecallTrackingService service = mock(RecallTrackingService.class);
    RecallController controller = new RecallController(service);
    User user = user();

    when(service.uncompleteRecall(user, 55L)).thenThrow(new RecallNotFoundException());

    assertEquals(HttpStatus.NOT_FOUND, controller.uncompleteRecall(user, 55L).getStatusCode());
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

  private RecallResponse recallResponse() {
    return new RecallResponse(
        22L,
        "JTENU5JR6M5962554",
        "22V480000",
        LocalDate.of(2022, 6, 7),
        "EQUIPMENT:OTHER:LABELS",
        "Summary",
        "Consequence",
        "Remedy");
  }
}
