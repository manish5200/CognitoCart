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

    // 1. SECURITY: Handles login failures
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "AUTH_FAILED", "Invalid email or password", null);
    }

    // 2. SECURITY: Handles disabled accounts
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabledAccount(DisabledException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "Your account has been deactivated.", null);
    }

    // 3. VALIDATION: Handles @Valid failures (e.g., missing email in DTO)
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

    // 4. DATA INTEGRITY: Handles unique DB constraints
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException e) {
        return buildErrorResponse(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", "A record with this unique data already exists.", null);
    }

    // 5. RESOURCE NOT FOUND (HTTP 404)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), null);
    }

    // 6. BUSINESS LOGIC ERRORS (HTTP 400)
    @ExceptionHandler(BusinessLogicException.class)
    public ResponseEntity<ErrorResponse> handleBusinessLogic(BusinessLogicException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "BUSINESS_ERROR", ex.getMessage(), null);
    }

    // 7. INSUFFICIENT STOCK / CONFLICTS (HTTP 409)
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleStockConflict(InsufficientStockException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, "STOCK_CONFLICT", ex.getMessage(), null);
    }

    // 8. CATCH-ALL FOR RAW RUNTIME EXCEPTIONS (Usually Token Expirations or lazy dev throws)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        if (ex.getMessage() != null && ex.getMessage().contains("Refresh token")) {
            return buildErrorResponse(HttpStatus.FORBIDDEN, "TOKEN_EXPIRED", ex.getMessage(), null);
        }
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "RUNTIME_ERROR", ex.getMessage(), null);
    }

    // 9. THE ULTIMATE FALLBACK: 500 Internal Server Error (Database unreachable, NullPointerExceptions)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "SYSTEM_ERROR", "An unexpected internal error occurred on the server.", null);
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
