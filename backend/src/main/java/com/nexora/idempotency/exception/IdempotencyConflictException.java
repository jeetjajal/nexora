package com.nexora.idempotency.exception;

/**
 * Thrown for either of two distinct idempotency problems:
 *   1. A request with this Idempotency-Key is still IN_PROGRESS
 *      (a genuinely concurrent duplicate arrived before the first
 *      attempt finished).
 *   2. The SAME key was reused with a materially DIFFERENT request
 *      (a client bug — keys must be unique per logical operation, not
 *      reused across unrelated requests).
 * Returned as 409 CONFLICT in both cases.
 */
public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String message) {
        super(message);
    }
}
