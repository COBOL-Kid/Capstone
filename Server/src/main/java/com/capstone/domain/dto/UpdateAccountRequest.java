package com.capstone.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateAccountRequest(
		@NotBlank(message = "First name is required") @Size(max = 100, message = "First name must be 100 characters or fewer") String firstName,
		@NotBlank(message = "Last name is required") @Size(max = 100, message = "Last name must be 100 characters or fewer") String lastName,
		@Size(max = 20, message = "SMS number must be 20 characters or fewer") @Pattern(regexp = "^[+0-9() .-]*$", message = "SMS number contains invalid characters") String userSms) {
}
