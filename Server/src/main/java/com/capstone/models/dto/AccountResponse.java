package com.capstone.models.dto;

import java.time.Instant;

public record AccountResponse(
    Long userId,
    String email,
    String firstName,
    String lastName,
    String userSms,
    boolean emailVerified,
    Instant emailVerifiedAt,
    Instant createdAt,
    Instant updatedAt) {}
