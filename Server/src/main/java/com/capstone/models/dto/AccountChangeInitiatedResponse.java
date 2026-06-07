package com.capstone.models.dto;

import com.capstone.models.AccountChangeType;

public record AccountChangeInitiatedResponse(AccountChangeType changeType, int expiresInMinutes) {}
