package com.capstone.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.capstone.domain.RecallTrackingService;
import com.capstone.domain.dto.CompleteRecallRequest;
import com.capstone.domain.dto.CompletedRecallResponse;
import com.capstone.models.User;

class RecallControllerTest {

	@Test
	void shouldRequireAuthentication() {
		RecallController controller = new RecallController(mock(RecallTrackingService.class));

		assertEquals(HttpStatus.UNAUTHORIZED,
				controller.getCompletedRecalls(null, "JTENU5JR6M5962554").getStatusCode());
		assertEquals(HttpStatus.UNAUTHORIZED,
				controller
						.completeRecall(null,
								new CompleteRecallRequest("JTENU5JR6M5962554", 22L, null, null, null, null))
						.getStatusCode());
	}

	@Test
	void shouldReturnNoContentWhenNoCompletedRecallsExist() {
		RecallTrackingService service = mock(RecallTrackingService.class);
		RecallController controller = new RecallController(service);
		User user = user();

		when(service.findCompletedRecalls(user, "JTENU5JR6M5962554")).thenReturn(List.of());

		assertEquals(HttpStatus.NO_CONTENT, controller.getCompletedRecalls(user, "JTENU5JR6M5962554").getStatusCode());
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
		CompleteRecallRequest request = new CompleteRecallRequest("JTENU5JR6M5962554", 22L, LocalDate.of(2025, 4, 6),
				"Toyota dealer", 0.0, "Airbag recall done");
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

	private CompletedRecallResponse completedRecall() {
		return new CompletedRecallResponse(55L, "JTENU5JR6M5962554", 22L, LocalDate.of(2025, 4, 6), "Toyota dealer",
				0.0, "Airbag recall done");
	}
}
