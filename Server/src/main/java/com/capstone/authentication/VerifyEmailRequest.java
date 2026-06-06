package com.capstone.authentication;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyEmailRequest(
    @NotBlank(message = "Verification code is required")
        @Pattern(regexp = "^\\d{6}$", message = "Verification code must be 6 digits")
        String code) {}
