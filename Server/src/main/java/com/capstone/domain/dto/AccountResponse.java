package com.capstone.domain.dto;

import java.time.LocalDateTime;

public record AccountResponse(Long userId, String email, String firstName, String lastName, String userSms,
		LocalDateTime createdAt, LocalDateTime updatedAt) {
}
