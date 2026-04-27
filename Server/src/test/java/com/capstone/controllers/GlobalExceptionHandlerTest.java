package com.capstone.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.HttpMediaTypeNotSupportedException;

import org.junit.jupiter.api.Test;

class GlobalExceptionHandlerTest {

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
    void shouldReturnGenericInternalServerErrorMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        var response = handler.handleException(new RuntimeException("Database unavailable"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Sometimes things just don't go as planned.", response.getBody());
    }
}
