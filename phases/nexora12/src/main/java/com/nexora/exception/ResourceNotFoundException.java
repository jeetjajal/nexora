package com.nexora.exception;

/**
 * Thrown when a lookup (e.g. findById) doesn't find anything.
 * GlobalExceptionHandler turns this into a clean 404 NOT_FOUND response.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
