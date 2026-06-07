package com.capstone.models.dto;

import com.capstone.models.AccountChangeType;

public record PendingAccountChangeResponse(AccountChangeType changeType, int expiresInMinutes) {}
