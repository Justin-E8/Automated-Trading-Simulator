package com.tradingsim.api.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Standardized API error payload used across controller exception handlers.
 *
 * <p>The {@code error} field is kept as a compatibility alias for existing UI parsing logic.</p>
 */
public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String code,
        String message,
        String error,
        String path,
        List<ValidationError> validationErrors
) {
    public ApiErrorResponse {
        validationErrors = validationErrors == null ? List.of() : List.copyOf(validationErrors);
    }

    /**
     * Field-level validation issue detail.
     */
    public record ValidationError(String field, String message) {
    }
}
