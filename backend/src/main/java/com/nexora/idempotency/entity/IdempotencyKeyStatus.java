package com.nexora.idempotency.entity;

public enum IdempotencyKeyStatus {
    /** The original request is still executing — a concurrent duplicate should be rejected, not replayed. */
    IN_PROGRESS,

    /** The original request finished successfully — a duplicate should get the cached response, not re-execute. */
    COMPLETED
}
