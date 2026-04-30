package com.capstone.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeleteAccountRequest(
		@NotBlank(message = "Password is required") @Size(max = 72, message = "Password must be 72 characters or fewer") String password) {
}
