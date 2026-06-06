package com.capstone.controllers;

import com.capstone.authentication.ExpiredEmailVerificationCodeException;
import com.capstone.authentication.InvalidEmailVerificationCodeException;
import com.capstone.authentication.InvalidRefreshTokenException;
import com.capstone.domain.*;
import com.capstone.integration.EmailDeliveryException;
import jakarta.validation.ConstraintViolationException;
import java.util.Comparator;
import java.util.List;
import org.hibernate.TypeMismatchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException ex) {
    List<ValidationErrorResponse.FieldValidationError> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .sorted(Comparator.comparing(FieldError::getField))
            .map(
                error ->
                    new ValidationErrorResponse.FieldValidationError(
                        error.getField(), error.getDefaultMessage()))
            .toList();
    return new ResponseEntity<>(
        new ValidationErrorResponse("Validation failed", errors), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ValidationErrorResponse> handleConstraintViolationException(
      ConstraintViolationException ex) {
    List<ValidationErrorResponse.FieldValidationError> errors =
        ex.getConstraintViolations().stream()
            .map(
                violation ->
                    new ValidationErrorResponse.FieldValidationError(
                        violation.getPropertyPath().toString(), violation.getMessage()))
            .sorted(Comparator.comparing(ValidationErrorResponse.FieldValidationError::field))
            .toList();
    return new ResponseEntity<>(
        new ValidationErrorResponse("Validation failed", errors), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(HttpMessageConversionException.class)
  public ResponseEntity<String> handleHttpMessageConversionException(
      HttpMessageConversionException ex) {
    log.debug("Malformed request body: {}", ex.getMessage());
    return new ResponseEntity<>("Malformed request body", HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(TypeMismatchException.class)
  public ResponseEntity<String> handleTypeMismatchException(TypeMismatchException ex) {
    log.debug("Type mismatch: {}", ex.getMessage());
    return new ResponseEntity<>("Malformed request body", HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<String> handleHttpRequestMethodNotSupportedException(
      HttpRequestMethodNotSupportedException ex) {
    log.debug("Method not allowed: {}", ex.getMessage());
    return new ResponseEntity<>("Method not allowed", HttpStatus.METHOD_NOT_ALLOWED);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<String> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
    return new ResponseEntity<>(ex.getMessage(), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
    return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(DuplicateEmailException.class)
  public ResponseEntity<String> handleDuplicateEmailException(DuplicateEmailException ex) {
    return new ResponseEntity<>(ex.getMessage(), HttpStatus.CONFLICT);
  }

  @ExceptionHandler(VinNotAssociatedException.class)
  public ResponseEntity<String> handleVinNotAssociatedException(VinNotAssociatedException ex) {
    return new ResponseEntity<>(ex.getMessage(), HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(EmailNotVerifiedException.class)
  public ResponseEntity<String> handleEmailNotVerifiedException(EmailNotVerifiedException ex) {
    return new ResponseEntity<>(ex.getMessage(), HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler({
    InvalidEmailVerificationCodeException.class,
    ExpiredEmailVerificationCodeException.class
  })
  public ResponseEntity<String> handleEmailVerificationCodeException(RuntimeException ex) {
    return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(EmailDeliveryException.class)
  public ResponseEntity<String> handleEmailDeliveryException(EmailDeliveryException ex) {
    log.warn("Email delivery failed: {}", ex.getMessage());
    return new ResponseEntity<>(
        "Unable to send verification email right now. Please try again later.",
        HttpStatus.SERVICE_UNAVAILABLE);
  }

  @ExceptionHandler({
    VinNotFoundException.class,
    MaintenanceItemNotFoundException.class,
    RecallNotFoundException.class
  })
  public ResponseEntity<String> handleNotFoundException(RuntimeException ex) {
    return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler({InvalidAccountCredentialsException.class, BadCredentialsException.class})
  public ResponseEntity<String> handleInvalidAccountCredentialsException(Exception ex) {
    return new ResponseEntity<>("Invalid account credentials", HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(InvalidRefreshTokenException.class)
  public ResponseEntity<String> handleInvalidRefreshTokenException(
      InvalidRefreshTokenException ex) {
    log.debug("Rejecting refresh: {}", ex.getMessage());
    ResponseCookie clearCookie =
        ResponseCookie.from("refreshToken", "")
            .httpOnly(true)
            .secure(true)
            .path("/api/auth")
            .maxAge(0)
            .sameSite("Strict")
            .build();
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.SET_COOKIE, clearCookie.toString());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).headers(headers).build();
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handleException(Exception ex) {
    log.error("Unhandled exception", ex);
    return new ResponseEntity<>(
        "Sometimes things just don't go as planned.", HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
