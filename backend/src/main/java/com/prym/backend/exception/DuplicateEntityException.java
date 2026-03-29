package com.prym.backend.exception;

// Thrown when trying to create an entity that already exists (maps to HTTP 409)
public class DuplicateEntityException extends RuntimeException {
    public DuplicateEntityException(String message) {
        super(message);
    }
}
