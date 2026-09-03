package com.purchaseplatform.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.HashMap;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(PurchaseNotFoundException.class)
        public ResponseEntity<Map<String, Object>> handlePurchaseNotFound(
                        PurchaseNotFoundException ex) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of(
                                                "timestamp", Instant.now(),
                                                "status", HttpStatus.NOT_FOUND.value(),
                                                "error", "Not Found",
                                                "message", ex.getMessage()));
        }

        @ExceptionHandler(ExchangeRateNotFoundException.class)
        public ResponseEntity<Map<String, Object>> handleExchangeRateNotFound(
                        ExchangeRateNotFoundException ex) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
                                .body(Map.of(
                                                "timestamp", Instant.now(),
                                                "status", HttpStatus.UNPROCESSABLE_CONTENT.value(),
                                                "error", "Unprocessable Entity",
                                                "message", ex.getMessage()));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, Object>> handleValidationErrors(
                        MethodArgumentNotValidException ex) {
                Map<String, String> validationErrors = new HashMap<>();

                ex.getBindingResult()
                                .getFieldErrors()
                                .forEach(error -> validationErrors.put(
                                                error.getField(),
                                                error.getDefaultMessage()));

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of(
                                                "timestamp", Instant.now(),
                                                "status", 400,
                                                "error", "Bad Request",
                                                "message", "Request validation failed",
                                                "validationErrors", validationErrors));
        }
}