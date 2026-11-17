package com.rentalhub.exception;

/**
 * Thrown for business rule violations and invalid input.
 * Maps to HTTP 400 in GlobalExceptionHandler.
 *
 * Usage:
 *   throw new BadRequestException("Cannot assign a tenant to an inactive property");
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}