package com.manish.smartcart.dto.error;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class ErrorResponse {
    private int status;            // HTTP Status Code (e.g., 400, 404)
    private String errorCode;      // Custom internal code (e.g., "USER_NOT_FOUND")
    private String message;        // Human-readable summary
    private LocalDateTime timestamp;
    private Map<String, String> validationErrors; // Stores field-specific errors (e.g., "email": "invalid")
}
