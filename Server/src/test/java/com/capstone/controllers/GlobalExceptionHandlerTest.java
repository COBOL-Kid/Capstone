package com.capstone.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RequestBody;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.capstone.models.dto.AddVinRequest;
import com.capstone.domain.DuplicateEmailException;
import com.capstone.domain.InvalidAccountCredentialsException;

import jakarta.validation.Valid;

class GlobalExceptionHandlerTest {

	@Test
	@DisplayName("should return structured bad request response for validation errors")
	void shouldReturnStructuredBadRequestForValidationErrors() throws Exception {
		GlobalExceptionHandler handler = new GlobalExceptionHandler();
		AddVinRequest request = new AddVinRequest("too-short", -1);
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(request, "request");
		bindingResult.addError(new FieldError("request", "vin", "VIN must be 17 characters"));
		MethodArgumentNotValidException exception = new MethodArgumentNotValidException(validationMethodParameter(),
				bindingResult);

		var response = handler.handleMethodArgumentNotValidException(exception);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("Validation failed", response.getBody().message());
		assertEquals("vin", response.getBody().errors().get(0).field());
		assertEquals("VIN must be 17 characters", response.getBody().errors().get(0).message());
	}

	@Test
	void shouldReturnBadRequestForMessageConversionErrors() {
		GlobalExceptionHandler handler = new GlobalExceptionHandler();

		var response = handler.handleHttpMessageConversionException(new HttpMessageConversionException("Bad JSON"));

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("Bad JSON", response.getBody());
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

		var response = handler.handleDuplicateEmailException(new DuplicateEmailException());

		assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
		assertEquals("Email is already registered", response.getBody());
	}

	@Test
	void shouldReturnUnauthorizedForInvalidAccountCredentials() {
		GlobalExceptionHandler handler = new GlobalExceptionHandler();

		var response = handler.handleInvalidAccountCredentialsException(new InvalidAccountCredentialsException());

		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
		assertEquals("Invalid account credentials", response.getBody());
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
	private void validationTarget(@Valid @RequestBody AddVinRequest request) {
	}
}
