package com.capstone.domain.dto;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.capstone.Authentication.AuthenticationRequest;
import com.capstone.Authentication.RegisterRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class RequestValidationTest {

	private static ValidatorFactory validatorFactory;
	private static Validator validator;

	@BeforeAll
	static void createValidator() {
		validatorFactory = Validation.buildDefaultValidatorFactory();
		validator = validatorFactory.getValidator();
	}

	@AfterAll
	static void closeValidator() {
		validatorFactory.close();
	}

	@Test
	void shouldValidateAuthenticationRequests() {
		assertTrue(validator.validate(new AuthenticationRequest("driver@example.com", "secret")).isEmpty());

		Set<String> messages = messages(new AuthenticationRequest("not-an-email", ""));

		assertTrue(messages.contains("Email must be valid"));
		assertTrue(messages.contains("Password is required"));
	}

	@Test
	void shouldValidateRegistrationRequests() {
		assertTrue(validator.validate(new RegisterRequest("Pat", "Driver", "driver@example.com", "LongSecret1!"))
				.isEmpty());

		Set<String> messages = messages(new RegisterRequest("", "", "bad", "short"));

		assertTrue(messages.contains("First name is required"));
		assertTrue(messages.contains("Last name is required"));
		assertTrue(messages.contains("Email must be valid"));
		assertTrue(messages.contains("Password must be between 8 and 72 characters"));
		assertTrue(messages.contains("Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"));
	}

	@Test
	void shouldValidateAccountRequests() {
		assertTrue(validator.validate(new UpdateAccountRequest("Pat", "Driver", "+1 (555) 123-4567")).isEmpty());
		assertTrue(validator.validate(new UpdateAccountRequest("Pat", "Driver", "")).isEmpty());
		assertTrue(validator.validate(new UpdateAccountRequest("Pat", "Driver", null)).isEmpty());
		assertTrue(validator.validate(new ChangePasswordRequest("old-secret", "new-secret")).isEmpty());
		assertTrue(validator.validate(new DeleteAccountRequest("secret")).isEmpty());

		Set<String> updateMessages = messages(new UpdateAccountRequest("", "", "abc<script>"));
		Set<String> noDigitsMessages = messages(new UpdateAccountRequest("Pat", "Driver", "()-. "));
		Set<String> passwordMessages = messages(new ChangePasswordRequest("", "short"));
		Set<String> deleteMessages = messages(new DeleteAccountRequest(""));

		assertTrue(updateMessages.contains("First name is required"));
		assertTrue(updateMessages.contains("Last name is required"));
		assertTrue(updateMessages.contains("SMS number contains invalid characters"));
		assertTrue(noDigitsMessages.contains("SMS number contains invalid characters"));
		assertTrue(passwordMessages.contains("Current password is required"));
		assertTrue(passwordMessages.contains("New password must be between 8 and 72 characters"));
		assertTrue(deleteMessages.contains("Password is required"));
	}

	@Test
	void shouldValidateAddVinRequests() {
		assertTrue(validator.validate(new AddVinRequest(" jtenu5jr6m5962554 ", 0)).isEmpty());

		Set<String> messages = messages(new AddVinRequest("too-short", -1));

		assertTrue(messages.contains(VinValidation.VIN_MESSAGE));
		assertTrue(messages.contains("Current mileage cannot be negative"));
		assertTrue(messages(new AddVinRequest("JTENU5JR6M5962554", null)).contains("Current mileage is required"));
	}

	@Test
	void shouldValidateCompleteMaintenanceRequests() {
		assertTrue(validator.validate(new CompleteMaintenanceRequest("JTENU5JR6M5962554", 11L, LocalDate.now(), 45100,
				120.50, "Dealer service")).isEmpty());

		Set<String> messages = messages(new CompleteMaintenanceRequest("JTI NU5JR6M596255", -1L,
				LocalDate.now().plusDays(1), null, -0.01, "x".repeat(1001)));

		assertTrue(messages.contains(VinValidation.VIN_MESSAGE));
		assertTrue(messages.contains("Maintenance item must be positive"));
		assertTrue(messages.contains("Completed date cannot be in the future"));
		assertTrue(messages.contains("Mileage completed is required"));
		assertTrue(messages.contains("Cost cannot be negative"));
		assertTrue(messages.contains("Notes must be 1000 characters or fewer"));
	}

	@Test
	void shouldValidateCompleteRecallRequests() {
		assertTrue(validator.validate(new CompleteRecallRequest("JTENU5JR6M5962554", 22L, LocalDate.now(),
				"Toyota dealer", 0.0, "Airbag recall done")).isEmpty());

		Set<String> messages = messages(new CompleteRecallRequest("JTENU5JR6M596255Q", null,
				LocalDate.now().plusDays(1), "x".repeat(256), -0.01, "x".repeat(1001)));

		assertTrue(messages.contains(VinValidation.VIN_MESSAGE));
		assertTrue(messages.contains("Recall is required"));
		assertTrue(messages.contains("Completed date cannot be in the future"));
		assertTrue(messages.contains("Repair shop must be 255 characters or fewer"));
		assertTrue(messages.contains("Cost cannot be negative"));
		assertTrue(messages.contains("Notes must be 1000 characters or fewer"));
	}

	private Set<String> messages(Object target) {
		return validator.validate(target).stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
	}
}
