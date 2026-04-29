package com.capstone.domain.dto;

import java.time.Instant;

public record AccountResponse(Long userId, String email, String firstName, String lastName, String userSms,
		Instant createdAt, Instant updatedAt) {
}
