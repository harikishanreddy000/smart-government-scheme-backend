package com.example.smart.government.scheme.controller;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import com.example.smart.government.scheme.dto.ApiError;
import com.example.smart.government.scheme.service.SchemeService.SchemeNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleInvalidRequest(IllegalArgumentException exception, WebRequest request) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleMalformedJson(HttpMessageNotReadableException exception, WebRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Request body must be valid JSON with the documented fields.", request);
    }

    @ExceptionHandler(SchemeNotFoundException.class)
    public ResponseEntity<ApiError> handleSchemeNotFound(SchemeNotFoundException exception, WebRequest request) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ApiError> handleOfficialCatalogFailure(RestClientException exception, WebRequest request) {
        return error(HttpStatus.BAD_GATEWAY,
                "The official myScheme catalogue is temporarily unavailable. Please retry in a moment.", request);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message, WebRequest request) {
        String path = request instanceof ServletWebRequest servletRequest
                ? servletRequest.getRequest().getRequestURI()
                : "";
        return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path));
    }
}
