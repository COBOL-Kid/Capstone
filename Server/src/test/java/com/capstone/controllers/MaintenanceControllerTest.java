package com.capstone.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.capstone.domain.MaintenanceTrackingService;
import com.capstone.models.dto.CompleteMaintenanceRequest;
import com.capstone.models.dto.CompletedMaintenanceResponse;
import com.capstone.models.dto.UpcomingMaintenanceResponse;
import com.capstone.models.User;

class MaintenanceControllerTest {

	@Test
	void shouldRequireAuthentication() {
		MaintenanceController controller = new MaintenanceController(mock(MaintenanceTrackingService.class));

		assertEquals(HttpStatus.UNAUTHORIZED,
				controller.getCompletedMaintenance(null, "JTENU5JR6M5962554").getStatusCode());
		assertEquals(HttpStatus.UNAUTHORIZED,
				controller.getUpcomingMaintenance(null, "JTENU5JR6M5962554").getStatusCode());
		assertEquals(HttpStatus.UNAUTHORIZED,
				controller
						.completeMaintenance(null,
								new CompleteMaintenanceRequest("JTENU5JR6M5962554", 11L, null, 1, null, null))
						.getStatusCode());
	}

	@Test
	void shouldReturnNoContentWhenNoCompletedMaintenanceExists() {
		MaintenanceTrackingService service = mock(MaintenanceTrackingService.class);
		MaintenanceController controller = new MaintenanceController(service);
		User user = user();

		when(service.findCompletedMaintenance(user, "JTENU5JR6M5962554")).thenReturn(List.of());

		assertEquals(HttpStatus.NO_CONTENT,
				controller.getCompletedMaintenance(user, "JTENU5JR6M5962554").getStatusCode());
	}

	@Test
	void shouldReturnNoContentWhenNoUpcomingMaintenanceExists() {
		MaintenanceTrackingService service = mock(MaintenanceTrackingService.class);
		MaintenanceController controller = new MaintenanceController(service);
		User user = user();

		when(service.findUpcomingMaintenance(user, "JTENU5JR6M5962554")).thenReturn(List.of());

		assertEquals(HttpStatus.NO_CONTENT,
				controller.getUpcomingMaintenance(user, "JTENU5JR6M5962554").getStatusCode());
	}

	@Test
	void shouldReturnCompletedMaintenance() {
		MaintenanceTrackingService service = mock(MaintenanceTrackingService.class);
		MaintenanceController controller = new MaintenanceController(service);
		User user = user();
		CompletedMaintenanceResponse completed = completedMaintenance();

		when(service.findCompletedMaintenance(user, "JTENU5JR6M5962554")).thenReturn(List.of(completed));

		var response = controller.getCompletedMaintenance(user, "JTENU5JR6M5962554");

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(List.of(completed), response.getBody());
	}

	@Test
	void shouldCreateCompletedMaintenance() {
		MaintenanceTrackingService service = mock(MaintenanceTrackingService.class);
		MaintenanceController controller = new MaintenanceController(service);
		User user = user();
		CompleteMaintenanceRequest request = new CompleteMaintenanceRequest("JTENU5JR6M5962554", 11L,
				LocalDate.of(2025, 4, 5), 45100, 120.50, "Dealer service");
		CompletedMaintenanceResponse completed = completedMaintenance();

		when(service.completeMaintenance(user, request)).thenReturn(completed);

		var response = controller.completeMaintenance(user, request);

		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertEquals(completed, response.getBody());
	}

	@Test
	void shouldReturnUpcomingMaintenance() {
		MaintenanceTrackingService service = mock(MaintenanceTrackingService.class);
		MaintenanceController controller = new MaintenanceController(service);
		User user = user();
		UpcomingMaintenanceResponse upcoming = new UpcomingMaintenanceResponse(11L, "JTENU5JR6M5962554", 45000, "Dealer service");

		when(service.findUpcomingMaintenance(user, "JTENU5JR6M5962554")).thenReturn(List.of(upcoming));

		var response = controller.getUpcomingMaintenance(user, "JTENU5JR6M5962554");

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(List.of(upcoming), response.getBody());
	}

	private User user() {
		User user = new User();
		user.setUserId(1L);
		user.setUserEmail("driver@example.com");
		return user;
	}

	private CompletedMaintenanceResponse completedMaintenance() {
		return new CompletedMaintenanceResponse(99L, "JTENU5JR6M5962554", 11L, LocalDate.of(2025, 4, 5), 45100, 120.50,
				"Dealer service");
	}
}
