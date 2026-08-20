package com.nexora.exception;

/**
 * Thrown when someone tries to register with an email that's already
 * in the "users" table. Kept as its own class (instead of a generic
 * RuntimeException) so GlobalExceptionHandler can catch it specifically
 * and return a clean 409 CONFLICT response.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("Email already registered: " + email);
    }
}
