package com.nexora.exception;

/**
 * Thrown when a user has the RIGHT ROLE for an operation (e.g. they
 * genuinely are a STORE_OWNER) but is trying to act on a resource they
 * don't own (e.g. editing another owner's store). This is distinct
 * from Spring Security's own 403 (wrong role entirely, handled by
 * JwtAccessDeniedHandler before the controller is even reached) —
 * this one fires INSIDE a service method, after we've loaded the
 * actual resource and compared its owner to the caller.
 */
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
