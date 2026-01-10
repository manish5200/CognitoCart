package com.manish.smartcart.exception;

import com.manish.smartcart.dto.error.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Handles validation errors (e.g., @NotBlank, @Min, @Size)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Input validation failed", errors);
    }

    // 2. Handles business logic errors (e.g., "Insufficient stock", "User not found")
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "BUSINESS_ERROR", ex.getMessage(), null);
    }

    // 3. Handle Database Integrity Errors (e.g., Duplicate Email, Invalid Foreign Keys)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException e) {
        return buildErrorResponse(HttpStatus.CONFLICT, "DATABASE_CONFLICT",
                "Data conflict error: Check for duplicates or invalid IDs.", null);
    }

    // 4. Fallback for unexpected system failures
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "SYSTEM_ERROR",
                "An unexpected error occurred: " + ex.getMessage(), null);
    }

    // 5. RECTIFIED: Standardized helper to return ErrorResponse DTO every time
    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String code, String message, Map<String, String> validationErrors) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(status.value())
                .errorCode(code)
                .message(message)
                .timestamp(LocalDateTime.now())
                .validationErrors(validationErrors)
                .build();
        return new ResponseEntity<>(errorResponse, status);
    }
}