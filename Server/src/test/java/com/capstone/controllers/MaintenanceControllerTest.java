package com.capstone.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.capstone.authentication.AuthenticatedUser;
import com.capstone.domain.MaintenanceItemNotFoundException;
import com.capstone.domain.MaintenanceTrackingService;
import com.capstone.models.Role;
import com.capstone.models.dto.CompleteMaintenanceRequest;
import com.capstone.models.dto.CompletedMaintenanceResponse;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MaintenanceControllerTest {

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

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
  void shouldPropagateNotFoundWhenUncompletingMissingMaintenance() {
    MaintenanceTrackingService service = mock(MaintenanceTrackingService.class);
    MaintenanceController controller = new MaintenanceController(service);
    AuthenticatedUser user = user();

    when(service.uncompleteMaintenance(1L, 99L)).thenThrow(new MaintenanceItemNotFoundException());

    assertThrows(
        MaintenanceItemNotFoundException.class, () -> controller.uncompleteMaintenance(user, 99L));
  }

  @Test
  void shouldReturnNotFoundThroughMvcAdviceWhenUncompletingMissingMaintenance() throws Exception {
    MaintenanceTrackingService service = mock(MaintenanceTrackingService.class);
    MaintenanceController controller = new MaintenanceController(service);
    AuthenticatedUser user = user();

    doThrow(new MaintenanceItemNotFoundException()).when(service).uncompleteMaintenance(1L, 99L);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));

    mockMvc(controller)
        .perform(delete("/api/maintenance/completed/99"))
        .andExpect(status().isNotFound())
        .andExpect(content().string("Maintenance item not found"));
  }

  private MockMvc mockMvc(MaintenanceController controller) {
    return MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(new GlobalExceptionHandler())
        .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
        .build();
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
