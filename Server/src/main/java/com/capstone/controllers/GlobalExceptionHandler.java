package com.capstone.controllers;

import java.util.Comparator;
import java.util.List;

import org.hibernate.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.validation.ConstraintViolationException;

import com.capstone.domain.DuplicateEmailException;
import com.capstone.domain.InvalidAccountCredentialsException;

@ControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValidException(
			MethodArgumentNotValidException ex) {
		List<ValidationErrorResponse.FieldValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
				.sorted(Comparator.comparing(FieldError::getField))
				.map(error -> new ValidationErrorResponse.FieldValidationError(error.getField(),
						error.getDefaultMessage()))
				.toList();
		return new ResponseEntity<>(new ValidationErrorResponse("Validation failed", errors), HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ValidationErrorResponse> handleConstraintViolationException(ConstraintViolationException ex) {
		List<ValidationErrorResponse.FieldValidationError> errors = ex.getConstraintViolations().stream()
				.map(violation -> new ValidationErrorResponse.FieldValidationError(
						violation.getPropertyPath().toString(), violation.getMessage()))
				.sorted(Comparator.comparing(ValidationErrorResponse.FieldValidationError::field)).toList();
		return new ResponseEntity<>(new ValidationErrorResponse("Validation failed", errors), HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(HttpMessageConversionException.class)
	public ResponseEntity<String> handleHttpMessageConversionException(HttpMessageConversionException ex) {
		return new ResponseEntity<String>(ex.getMessage(), HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(TypeMismatchException.class)
	public ResponseEntity<String> handleTypeMismatchException(TypeMismatchException ex) {
		return new ResponseEntity<String>(ex.getMessage(), HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<String> handleHttpRequestMethodNotSupportedException(
			HttpRequestMethodNotSupportedException ex) {
		return new ResponseEntity<String>(ex.getMessage(), HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<String> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
		return new ResponseEntity<String>(ex.getMessage(), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
		return new ResponseEntity<String>(ex.getMessage(), HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(DuplicateEmailException.class)
	public ResponseEntity<String> handleDuplicateEmailException(DuplicateEmailException ex) {
		return new ResponseEntity<String>(ex.getMessage(), HttpStatus.CONFLICT);
	}

	@ExceptionHandler({InvalidAccountCredentialsException.class, BadCredentialsException.class})
	public ResponseEntity<String> handleInvalidAccountCredentialsException(Exception ex) {
		return new ResponseEntity<String>("Invalid account credentials", HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<String> handleException(Exception ex) {
		return new ResponseEntity<String>("Sometimes things just don't go as planned.",
				HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
