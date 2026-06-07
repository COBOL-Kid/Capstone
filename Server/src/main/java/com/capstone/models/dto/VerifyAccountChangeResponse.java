package com.capstone.models.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VerifyAccountChangeResponse(
    AccountResponse account, String token, Boolean emailVerified) {}
