package com.project.backend.Exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiError.class)
    public ResponseEntity<ApiErrorResponse> handleApiError(ApiError ex, HttpServletRequest request) {
        logByStatus(ex);
        return buildResponse(ex, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.putIfAbsent(fieldError.getField(), resolveMessage(fieldError));
        }

        String message = details.entrySet()
                .stream()
                .findFirst()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .orElse("Validation failed");

        return buildResponse(ApiError.validation(message, details), request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();

        ex.getConstraintViolations().forEach(violation ->
                details.put(violation.getPropertyPath().toString(), violation.getMessage()));

        String message = details.entrySet()
                .stream()
                .findFirst()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .orElse("Validation failed");

        return buildResponse(ApiError.validation(message, details), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidBody(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        log.warn("Invalid request body at {}", request.getRequestURI(), ex);
        return buildResponse(ApiError.badRequest("Request body is invalid"), request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {
        return buildResponse(ApiError.badRequest(ex.getParameterName() + " parameter is required"), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        return buildResponse(ApiError.badRequest(ex.getName() + " parameter has invalid value"), request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(
            ResponseStatusException ex,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        HttpStatus resolvedStatus = status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status;
        String message = ex.getReason() == null ? resolvedStatus.getReasonPhrase() : ex.getReason();

        return buildResponse(new ApiError(resolvedStatus, codeFromStatus(resolvedStatus), message, ex), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(
            AuthenticationException ex,
            HttpServletRequest request) {
        return buildResponse(ApiError.unauthorized("Authentication is required"), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {
        return buildResponse(ApiError.forbidden("Access denied"), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {
        log.warn("Database constraint violation at {}", request.getRequestURI(), ex);
        return buildResponse(ApiError.conflict("Database constraint violation"), request);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiErrorResponse> handleDataAccess(
            DataAccessException ex,
            HttpServletRequest request) {
        log.error("Database error at {}", request.getRequestURI(), ex);
        return buildResponse(ApiError.database("Database operation failed", ex), request);
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<ApiErrorResponse> handleAsyncTimeout(
            AsyncRequestTimeoutException ex,
            HttpServletRequest request) {
        log.warn("Async request timed out at {}", request.getRequestURI(), ex);
        return buildResponse(
                new ApiError(HttpStatus.SERVICE_UNAVAILABLE, "REQUEST_TIMEOUT", "Request timed out", ex),
                request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error at {}", request.getRequestURI(), ex);
        return buildResponse(ApiError.internal("Internal server error", ex), request);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(ApiError apiError, HttpServletRequest request) {
        return ResponseEntity
                .status(apiError.getStatus())
                .body(ApiErrorResponse.from(apiError, request.getRequestURI()));
    }

    private String resolveMessage(FieldError fieldError) {
        return fieldError.getDefaultMessage() == null
                ? "invalid value"
                : fieldError.getDefaultMessage();
    }

    private void logByStatus(ApiError ex) {
        if (ex.getStatus().is5xxServerError()) {
            log.error("API error: {}", ex.getMessage(), ex);
        } else {
            log.warn("API error: {}", ex.getMessage());
        }
    }

    private String codeFromStatus(HttpStatus status) {
        return status.name();
    }
}
