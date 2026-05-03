package com.capstone.controllers;

import java.util.List;

public record ValidationErrorResponse(String message, List<FieldValidationError> errors) {

    public record FieldValidationError(String field, String message) {
    }
}
