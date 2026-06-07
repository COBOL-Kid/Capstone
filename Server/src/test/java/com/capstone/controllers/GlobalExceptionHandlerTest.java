package com.capstone.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.capstone.authentication.InvalidRefreshTokenException;
import com.capstone.domain.AccountChangeRequiredException;
import com.capstone.domain.DuplicateEmailException;
import com.capstone.domain.EmailNotVerifiedException;
import com.capstone.domain.InvalidAccountCredentialsException;
import com.capstone.domain.MaintenanceItemNotFoundException;
import com.capstone.domain.PendingAccountChangeNotFoundException;
import com.capstone.domain.RecallNotFoundException;
import com.capstone.domain.VinNotAssociatedException;
import com.capstone.domain.VinNotFoundException;
import com.capstone.email.EmailDeliveryException;
import com.capstone.email.InvalidEmailVerificationCodeException;
import com.capstone.models.dto.AddVinRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import java.lang.reflect.Method;
import java.util.Set;
import org.hibernate.TypeMismatchException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RequestBody;

class GlobalExceptionHandlerTest {

  @Test
  @DisplayName("should return structured bad request response for validation errors")
  void shouldReturnStructuredBadRequestForValidationErrors() throws Exception {
    GlobalExceptionHandler handler = new GlobalExceptionHandler();
    AddVinRequest request = new AddVinRequest("too-short", -1, null);
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(request, "request");
    bindingResult.addError(new FieldError("request", "vin", "VIN must be 17 characters"));
    MethodArgumentNotValidException exception =
        new MethodArgumentNotValidException(validationMethodParameter(), bindingResult);

    var response = handler.handleMethodArgumentNotValidException(exception);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("Validation failed", response.getBody().message());
    assertEquals("vin", response.getBody().errors().getFirst().field());
    assertEquals("VIN must be 17 characters", response.getBody().errors().getFirst().message());
  }

  @Test
  void shouldReturnBadRequestForMessageConversionErrors() {
    GlobalExceptionHandler handler = new GlobalExceptionHandler();

    var response =
        handler.handleMalformedRequestBody(new HttpMessageConversionException("Bad JSON"));

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("Malformed request body", response.getBody());
  }

  @Test
  void shouldReturnUnsupportedMediaTypeForMediaTypeErrors() {
    GlobalExceptionHandler handler = new GlobalExceptionHandler();
    HttpMediaTypeNotSupportedException exception = mock(HttpMediaTypeNotSupportedException.class);

    when(exception.getMessage()).thenReturn("Unsupported media type");

    var response = handler.handleUnsupportedMediaType(exception);

    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());
    assertEquals("Unsupported media type", response.getBody());
  }

  @Test
  void shouldReturnConflictForDuplicateEmail() {
    GlobalExceptionHandler handler = new GlobalExceptionHandler();

    var response = handler.handleConflictException(new DuplicateEmailException());

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertEquals("Email is already registered", response.getBody());
  }

  @Test
  void shouldReturnConflictForAccountChangeRequired() {
    GlobalExceptionHandler handler = new GlobalExceptionHandler();

    var response = handler.handleConflictException(new AccountChangeRequiredException());

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertEquals("Use account change verification to update your password", response.getBody());
  }

  @Test
  void shouldReturnUnauthorizedForInvalidAccountCredentials() {
    GlobalExceptionHandler handler = new GlobalExceptionHandler();

    var response =
        handler.handleInvalidAccountCredentialsException(new InvalidAccountCredentialsException());

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertEquals("Invalid account credentials", response.getBody());
  }

  @Test
  void shouldReturnNotFoundForVinNotFound() {
    GlobalExceptionHandler handler = new GlobalExceptionHandler();

    var response = handler.handleNotFoundException(new VinNotFoundException());

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertEquals("Vehicle data is not available for this VIN", response.getBody());
  }

  @Test
  void shouldReturnForbiddenForVinNotAssociated() {
    GlobalExceptionHandler handler = new GlobalExceptionHandler();

    var response = handler.handleForbiddenException(new VinNotAssociatedException());

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertEquals("VIN is not associated with this user", response.getBody());
  }

  @Test
  void shouldReturnForbiddenForEmailNotVerified() {
    GlobalExceptionHandler handler = new GlobalExceptionHandler();

    var response = handler.handleForbiddenException(new EmailNotVerifiedException());

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertEquals("Email address must be verified before adding vehicles", response.getBody());
  }

  @Test
  void shouldReturnNotFoundForMaintenanceItemNotFound() {
    GlobalExceptionHandler handler = new GlobalExceptionHandler();

    var response = handler.handleNotFoundException(new MaintenanceItemNotFoundException());

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertEquals("Maintenance item not found", response.getBody());
  }

  @Test
  void shouldReturnNotFoundForRecallNotFound() {
    GlobalExceptionHandler handler = new GlobalExceptionHandler();

    var response = handler.handleNotFoundException(new RecallNotFoundException());

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertEquals("Recall not found", response.getBody());
  }

  @Test
  void shouldReturnNotFoundForPendingAccountChangeNotFound() {
    GlobalExceptionHandler handler = new GlobalExceptionHandler();

    var response = handler.handleNotFoundException(new PendingAccountChangeNotFoundException());

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertEquals("No pending account change request", response.getBody());
  }

  @Test
  void shouldReturnBadRequestForInvalidEmailVerificationCode() {
    GlobalExceptionHandler handler = new GlobalExceptionHandler();

    var response =
        handler.handleEmailVerificationCodeException(new InvalidEmailVerificationCodeException());

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("Invalid verification code", response.getBody());
  }

  @Test
  void shouldReturnServiceUnavailableForEmailDeliveryFailure() {
    GlobalExceptionHandler handler = new GlobalExceptionHandler();

    var response =
        handler.handleEmailDeliveryException(new EmailDeliveryException("Mailjet API unavailable"));

    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    assertEquals(
        "Unable to send verification email right now. Please try again later.", response.getBody());
  }

  @Test
  void shouldClearRefreshTokenCookieForInvalidRefreshToken() {
    GlobalExceptionHandler handler = new GlobalExceptionHandler();

    var response =
        handler.handleInvalidRefreshTokenException(new InvalidRefreshTokenException("expired"));

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
    assertTrue(setCookie != null && setCookie.contains("refreshToken="));
    assertTrue(setCookie.contains("Max-Age=0"));
  }

  @Test
  void shouldReturnBadRequestForConstraintViolation() {
    GlobalExceptionHandler handler = new GlobalExceptionHandler();
    @SuppressWarnings("unchecked")
    ConstraintViolation<AddVinRequest> violation = mock(ConstraintViolation.class);
    when(violation.getPropertyPath()).thenReturn(mock(jakarta.validation.Path.class));
    when(violation.getPropertyPath().toString()).thenReturn("vin");
    when(violation.getMessage()).thenReturn("VIN must be 17 characters");

    var response =
        handler.handleConstraintViolationException(
            new ConstraintViolationException(Set.of(violation)));

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("vin", response.getBody().errors().getFirst().field());
  }

  @Test
  void shouldReturnBadRequestForIllegalArgument() {
    GlobalExceptionHandler handler = new GlobalExceptionHandler();

    var response =
        handler.handleIllegalArgumentException(new IllegalArgumentException("bad input"));

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("bad input", response.getBody());
  }

  @Test
  void shouldReturnUnauthorizedForBadCredentials() {
    GlobalExceptionHandler handler = new GlobalExceptionHandler();

    var response =
        handler.handleInvalidAccountCredentialsException(new BadCredentialsException("bad"));

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertEquals("Invalid account credentials", response.getBody());
  }

  @Test
  void shouldReturnBadRequestForTypeMismatch() {
    GlobalExceptionHandler handler = new GlobalExceptionHandler();
    TypeMismatchException exception = mock(TypeMismatchException.class);
    when(exception.getMessage()).thenReturn("type mismatch");

    var response = handler.handleMalformedRequestBody(exception);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("Malformed request body", response.getBody());
  }

  @Test
  void shouldReturnMethodNotAllowed() {
    GlobalExceptionHandler handler = new GlobalExceptionHandler();

    var response =
        handler.handleHttpRequestMethodNotSupportedException(
            new HttpRequestMethodNotSupportedException("POST"));

    assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
    assertEquals("Method not allowed", response.getBody());
  }

  @Test
  void shouldReturnGenericInternalServerErrorMessage() {
    GlobalExceptionHandler handler = new GlobalExceptionHandler();

    var response = handler.handleException(new RuntimeException("Database unavailable"));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals("Sometimes things just don't go as planned.", response.getBody());
  }

  private MethodParameter validationMethodParameter() throws NoSuchMethodException {
    Method method = getClass().getDeclaredMethod("validationTarget", AddVinRequest.class);
    return new MethodParameter(method, 0);
  }

  @SuppressWarnings("unused")
  private void validationTarget(@Valid @RequestBody AddVinRequest request) {}
}
