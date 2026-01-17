package com.manish.smartcart.exception;

import com.manish.smartcart.dto.error.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. SECURITY: Handles login failures (Wrong password/email)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "AUTH_FAILED",
                "Invalid email or password", null);
    }

    // 2. SECURITY: Handles blocked/disabled accounts
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabledAccount(DisabledException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED",
                "Your account has been deactivated. Please contact support.", null);
    }

    // 3. VALIDATION: Handles @Valid failures
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

    // 4. DATA INTEGRITY: Handles unique constraint violations (Email/Phone)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException e) {
        // Real-world: In production, we parse the DB message to tell the user EXACTLY what's duplicated
        String message = "A record with this data already exists (Duplicate Email/Phone).";
        return buildErrorResponse(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", message, null);
    }

    // 5. BUSINESS LOGIC: Catch-all for service-layer RuntimeExceptions
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        // If it's a Refresh Token error, we use 403 Forbidden
        if (ex.getMessage().contains("Refresh token")) {
            return buildErrorResponse(HttpStatus.FORBIDDEN, "TOKEN_EXPIRED", ex.getMessage(), null);
        }
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "BUSINESS_ERROR", ex.getMessage(), null);
    }

    // 6. SYSTEM FALLBACK
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "SYSTEM_ERROR",
                "An unexpected internal error occurred.", null);
    }

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