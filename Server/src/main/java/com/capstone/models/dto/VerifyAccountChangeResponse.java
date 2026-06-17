package com.capstone.models.dto;

import com.capstone.models.AccountChangeType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VerifyAccountChangeResponse(
    AccountResponse account,
    @JsonIgnore String token,
    Boolean emailVerified,
    AccountChangeType changeType) {}
