package com.capstone.models.dto;

import com.capstone.models.AccountChangeType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record InitiateAccountChangeRequest(
    @NotNull(message = "Change type is required") AccountChangeType changeType,
    @Email(message = "Email must be valid")
        @Size(max = 254, message = "Email must be 254 characters or fewer")
        String newEmail,
    @Size(max = 72, message = "Current password must be 72 characters or fewer")
        String currentPassword,
    @Size(min = 8, max = 72, message = "New password must be between 8 and 72 characters")
        String newPassword,
    @Size(max = 20, message = "SMS number must be 20 characters or fewer")
        @Pattern(
            regexp = "^$|^(?=.*\\d)[+0-9() .-]+$",
            message = "SMS number contains invalid characters")
        String userSms) {}
