package com.prym.backend.exception;

// Thrown when a requested entity does not exist in the database (maps to HTTP 404)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
