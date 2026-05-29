package com.capstone.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.capstone.authentication.AuthenticatedUser;
import com.capstone.domain.MaintenanceItemNotFoundException;
import com.capstone.domain.MaintenanceTrackingService;
import com.capstone.models.Role;
import com.capstone.models.dto.CompleteMaintenanceRequest;
import com.capstone.models.dto.CompletedMaintenanceResponse;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class MaintenanceControllerTest {

  @Test
  void shouldRequireAuthentication() {
    MaintenanceController controller =
        new MaintenanceController(mock(MaintenanceTrackingService.class));

    assertEquals(
        HttpStatus.UNAUTHORIZED,
        controller
            .completeMaintenance(
                null, new CompleteMaintenanceRequest("JTENU5JR6M5962554", 11L, null, 1, null, null))
            .getStatusCode());
    assertEquals(
        HttpStatus.UNAUTHORIZED, controller.uncompleteMaintenance(null, 99L).getStatusCode());
  }

  @Test
  void shouldUncompleteMaintenance() {
    MaintenanceTrackingService service = mock(MaintenanceTrackingService.class);
    MaintenanceController controller = new MaintenanceController(service);
    AuthenticatedUser user = user();

    when(service.uncompleteMaintenance(1L, 99L)).thenReturn(true);

    assertEquals(
        HttpStatus.NO_CONTENT, controller.uncompleteMaintenance(user, 99L).getStatusCode());
  }

  @Test
  void shouldCreateCompletedMaintenance() {
    MaintenanceTrackingService service = mock(MaintenanceTrackingService.class);
    MaintenanceController controller = new MaintenanceController(service);
    AuthenticatedUser user = user();
    CompleteMaintenanceRequest request =
        new CompleteMaintenanceRequest(
            "JTENU5JR6M5962554", 11L, LocalDate.of(2025, 4, 5), 45100, 120.50, "Dealer service");
    CompletedMaintenanceResponse completed = completedMaintenance();

    when(service.completeMaintenance(1L, request)).thenReturn(completed);

    var response = controller.completeMaintenance(user, request);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(completed, response.getBody());
  }

  @Test
  void shouldReturnNotFoundWhenUncompletingMissingMaintenance() {
    MaintenanceTrackingService service = mock(MaintenanceTrackingService.class);
    MaintenanceController controller = new MaintenanceController(service);
    AuthenticatedUser user = user();

    when(service.uncompleteMaintenance(1L, 99L)).thenThrow(new MaintenanceItemNotFoundException());

    assertEquals(HttpStatus.NOT_FOUND, controller.uncompleteMaintenance(user, 99L).getStatusCode());
  }

  private AuthenticatedUser user() {
    return new AuthenticatedUser(1L, "driver@example.com", Role.USER);
  }

  private CompletedMaintenanceResponse completedMaintenance() {
    return new CompletedMaintenanceResponse(
        99L,
        "JTENU5JR6M5962554",
        11L,
        LocalDate.of(2025, 4, 5),
        45100,
        120.50,
        "Dealer service",
        "Replace engine oil",
        50000);
  }
}
