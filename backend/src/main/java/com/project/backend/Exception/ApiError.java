package com.project.backend.Exception;

import java.util.Collections;
import java.util.Map;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class ApiError extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final Map<String, String> details;

    public ApiError(HttpStatus status, String code, String message) {
        this(status, code, message, null, Map.of());
    }

    public ApiError(HttpStatus status, String code, String message, Throwable cause) {
        this(status, code, message, cause, Map.of());
    }

    public ApiError(
            HttpStatus status,
            String code,
            String message,
            Throwable cause,
            Map<String, String> details) {
        super(message, cause);
        this.status = status;
        this.code = code;
        this.details = Collections.unmodifiableMap(details == null ? Map.of() : details);
    }

    public static ApiError badRequest(String message) {
        return new ApiError(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }

    public static ApiError validation(String message, Map<String, String> details) {
        return new ApiError(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, null, details);
    }

    public static ApiError unauthorized(String message) {
        return new ApiError(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
    }

    public static ApiError forbidden(String message) {
        return new ApiError(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }

    public static ApiError notFound(String message) {
        return new ApiError(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    public static ApiError conflict(String message) {
        return new ApiError(HttpStatus.CONFLICT, "CONFLICT", message);
    }

    public static ApiError tooManyRequests(String message) {
        return new ApiError(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS", message);
    }

    public static ApiError database(String message, Throwable cause) {
        return new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "DATABASE_ERROR", message, cause);
    }

    public static ApiError internal(String message, Throwable cause) {
        return new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", message, cause);
    }
}
