package com.tradingsim.api;

import com.tradingsim.api.dto.ApiErrorResponse;
import com.tradingsim.application.ResourceNotFoundException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.ConstraintViolation;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import jakarta.servlet.http.HttpServletRequest;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Centralized API error translation used by Spring MVC exception handling.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleNotFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return errorResponse(
                HttpStatus.NOT_FOUND,
                "NOT_FOUND",
                exception.getMessage(),
                request,
                List.of()
        );
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class,
            MultipartException.class,
            MaxUploadSizeExceededException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleBadRequest(Exception exception, HttpServletRequest request) {
        return errorResponse(
                HttpStatus.BAD_REQUEST,
                "BAD_REQUEST",
                exception.getMessage(),
                request,
                List.of()
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        List<ApiErrorResponse.ValidationError> validationErrors = exception.getConstraintViolations().stream()
                .map(violation -> new ApiErrorResponse.ValidationError(
                        fieldFromPath(violation),
                        violation.getMessage()
                ))
                .toList();
        String message = validationErrors.isEmpty()
                ? "Validation failed."
                : validationErrors.get(0).message();
        return errorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request, validationErrors);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<ApiErrorResponse.ValidationError> validationErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toValidationError)
                .toList();
        String message = validationErrors.isEmpty()
                ? "Validation failed."
                : validationErrors.get(0).message();
        return errorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request, validationErrors);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiErrorResponse handleInternalError(Exception exception, HttpServletRequest request) {
        return errorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "Unexpected server error.",
                request,
                List.of()
        );
    }

    private ApiErrorResponse.ValidationError toValidationError(FieldError error) {
        String message = error.getDefaultMessage() == null ? "Invalid value." : error.getDefaultMessage();
        return new ApiErrorResponse.ValidationError(error.getField(), message);
    }

    private String fieldFromPath(ConstraintViolation<?> violation) {
        Path propertyPath = violation.getPropertyPath();
        String lastNode = null;
        for (Path.Node node : propertyPath) {
            lastNode = node.getName();
        }
        return lastNode == null ? propertyPath.toString() : lastNode;
    }

    private ApiErrorResponse errorResponse(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            List<ApiErrorResponse.ValidationError> validationErrors
    ) {
        String resolvedMessage = message == null || message.isBlank() ? "Request failed." : message;
        String path = request == null ? "unknown" : request.getRequestURI();
        return new ApiErrorResponse(
                OffsetDateTime.now(ZoneOffset.UTC),
                status.value(),
                code,
                resolvedMessage,
                resolvedMessage,
                path,
                validationErrors
        );
    }
}
