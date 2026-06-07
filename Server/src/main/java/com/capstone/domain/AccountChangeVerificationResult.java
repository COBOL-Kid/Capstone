package com.capstone.domain;

import com.capstone.authentication.AuthenticationResponse;
import com.capstone.models.dto.AccountResponse;

public record AccountChangeVerificationResult(
    AccountResponse account, AuthenticationResponse session) {}
