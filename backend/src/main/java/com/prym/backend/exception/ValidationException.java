package com.prym.backend.exception;

// Thrown when input fails business rule validation (maps to HTTP 400)
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
