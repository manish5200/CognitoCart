package com.manish.smartcart.exception;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handles specific RuntimeExceptions (Stock, Not Found, etc.)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException e){
         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                 "timestamp", LocalDateTime.now(),
                 "error", e.getMessage(),
                 "status", HttpStatus.BAD_REQUEST.value()
         ));
    }

    // Handles unexpected generic errors
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception ex){
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "timestamp", LocalDateTime.now(),
                "error",ex.getMessage(),
                "status", HttpStatus.INTERNAL_SERVER_ERROR.value()
        ));
    }
}
