package com.rentalhub.exception;

/**
 * Thrown when a requested entity does not exist in the database.
 * Maps to HTTP 404 in GlobalExceptionHandler.
 *
 * Usage:
 *   throw new ResourceNotFoundException("Property", id);
 *   → "Property with id 5 not found"
 *
 *   throw new ResourceNotFoundException("No active agreement for property 3");
 *   → custom message
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String entityName, Long id) {
        super(entityName + " with id " + id + " not found");
    }

    public ResourceNotFoundException(String entityName, String field, String value) {
        super(entityName + " with " + field + " '" + value + "' not found");
    }
}