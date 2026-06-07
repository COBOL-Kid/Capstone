package com.capstone.controllers;

import com.capstone.authentication.AuthCookies;
import com.capstone.authentication.InvalidRefreshTokenException;
import com.capstone.domain.ConflictException;
import com.capstone.domain.ForbiddenAccessException;
import com.capstone.domain.InvalidAccountCredentialsException;
import com.capstone.domain.ResourceNotFoundException;
import com.capstone.domain.TooManyRequestsException;
import com.capstone.email.EmailDeliveryException;
import com.capstone.email.EmailVerificationCodeException;
import com.capstone.logging.RequestContextMdc;
import jakarta.validation.ConstraintViolationException;
import java.util.Comparator;
import java.util.List;
import org.hibernate.TypeMismatchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private final AuthCookies authCookies;

  public GlobalExceptionHandler(AuthCookies authCookies) {
    this.authCookies = authCookies;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException ex) {
    List<ValidationErrorResponse.FieldValidationError> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(
                error ->
                    new ValidationErrorResponse.FieldValidationError(
                        error.getField(), error.getDefaultMessage()))
            .sorted(Comparator.comparing(ValidationErrorResponse.FieldValidationError::field))
            .toList();
    return validationFailed(errors);
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
    return validationFailed(errors);
  }

  @ExceptionHandler({HttpMessageConversionException.class, TypeMismatchException.class})
  public ResponseEntity<String> handleMalformedRequestBody(Exception ex) {
    log.debug("Malformed request: {}", ex.getMessage());
    return textResponse("Malformed request body", HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<String> handleHttpRequestMethodNotSupportedException(
      HttpRequestMethodNotSupportedException ex) {
    log.debug("Method not allowed: {}", ex.getMessage());
    return textResponse("Method not allowed", HttpStatus.METHOD_NOT_ALLOWED);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<String> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
    return textResponse(ex.getMessage(), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
    return textResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<String> handleConflictException(ConflictException ex) {
    return textResponse(ex.getMessage(), HttpStatus.CONFLICT);
  }

  @ExceptionHandler(ForbiddenAccessException.class)
  public ResponseEntity<String> handleForbiddenException(ForbiddenAccessException ex) {
    return textResponse(ex.getMessage(), HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(EmailVerificationCodeException.class)
  public ResponseEntity<String> handleEmailVerificationCodeException(
      EmailVerificationCodeException ex) {
    return textResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(EmailDeliveryException.class)
  public ResponseEntity<String> handleEmailDeliveryException(EmailDeliveryException ex) {
    log.warn("Email delivery failed: {}", ex.getMessage());
    return textResponse(
        "Unable to send verification email right now. Please try again later.",
        HttpStatus.SERVICE_UNAVAILABLE);
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<String> handleNotFoundException(ResourceNotFoundException ex) {
    return textResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler({InvalidAccountCredentialsException.class, BadCredentialsException.class})
  public ResponseEntity<String> handleInvalidAccountCredentialsException(Exception ex) {
    return textResponse("Invalid account credentials", HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(LockedException.class)
  public ResponseEntity<String> handleLockedException(LockedException ex) {
    return textResponse("Account is locked", HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(TooManyRequestsException.class)
  public ResponseEntity<String> handleTooManyRequestsException(TooManyRequestsException ex) {
    return textResponse(ex.getMessage(), HttpStatus.TOO_MANY_REQUESTS);
  }

  @ExceptionHandler(InvalidRefreshTokenException.class)
  public ResponseEntity<String> handleInvalidRefreshTokenException(
      InvalidRefreshTokenException ex) {
    log.debug("Rejecting refresh: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .headers(authCookies.clearSessionCookies())
        .build();
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handleException(Exception ex) {
    log.error(
        "Unhandled exception requestId={} exceptionType={}",
        RequestContextMdc.requestId(),
        ex.getClass().getName(),
        ex);
    return textResponse(
        "Sometimes things just don't go as planned.", HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private static ResponseEntity<ValidationErrorResponse> validationFailed(
      List<ValidationErrorResponse.FieldValidationError> errors) {
    return new ResponseEntity<>(
        new ValidationErrorResponse("Validation failed", errors), HttpStatus.BAD_REQUEST);
  }

  private static ResponseEntity<String> textResponse(String body, HttpStatus status) {
    return new ResponseEntity<>(body, status);
  }
}
