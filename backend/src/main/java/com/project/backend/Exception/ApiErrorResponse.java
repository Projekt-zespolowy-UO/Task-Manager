package com.project.backend.Exception;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        Map<String, String> details) {

    public static ApiErrorResponse from(ApiError apiError, String path) {
        return of(
                apiError.getStatus(),
                apiError.getCode(),
                apiError.getMessage(),
                path,
                apiError.getDetails());
    }

    public static ApiErrorResponse of(
            HttpStatus status,
            String code,
            String message,
            String path,
            Map<String, String> details) {
        return new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                path,
                details == null ? Map.of() : details);
    }
}
