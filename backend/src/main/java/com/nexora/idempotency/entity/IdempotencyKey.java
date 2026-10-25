package com.nexora.idempotency.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * THE ROADMAP'S CORE PHASE 11 SCENARIO: a customer submits a
 * money-moving request (checkout, initiate payment), the network
 * fails before their browser sees the response, and they retry. Did
 * the first request actually succeed server-side? The client has no
 * way to know — and blindly retrying a plain POST would create a
 * second order, a second Razorpay order, or worse.
 *
 * THE FIX: the client sends a self-generated Idempotency-Key header
 * (any unique string — a UUID is the usual choice) with the request.
 * The FIRST time we see a given key, we run the real operation and
 * cache its result here. Every SUBSEQUENT request with that SAME key
 * gets the cached result replayed back — without re-running the
 * operation at all. The retry is safe by construction, not because we
 * were clever about detecting "is this a duplicate order," but because
 * we never run the operation a second time in the first place.
 *
 * WHY A UNIQUE DATABASE CONSTRAINT, NOT JUST AN APPLICATION-LEVEL CHECK:
 * imagine the "network failure" wasn't total — the first request is
 * still genuinely in flight on the server when the retry arrives
 * (e.g. a slow database round trip). An application-level
 * "does a record exist?" check has a race: both requests could check
 * at nearly the same instant, both see "no record," and both proceed
 * to run the real operation. The UNIQUE constraint on
 * (key_value, scope, user_id) closes that race at the database level:
 * only ONE of two simultaneous INSERT attempts for the same key can
 * ever succeed, no matter how the application-level timing lines up —
 * the same principle Phase 6 relied on for atomic inventory updates.
 */
@Entity
@Table(
        name = "idempotency_keys",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_idempotency_key_scope_user",
                        columnNames = {"key_value", "scope", "user_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The client-supplied Idempotency-Key header value (e.g. a UUID). */
    @Column(name = "key_value", nullable = false, length = 100)
    private String keyValue;

    /**
     * Which operation this key applies to (e.g. "CHECKOUT",
     * "PAYMENT_INITIATE") — the same key string could theoretically be
     * reused by a client across genuinely different endpoints; scoping
     * prevents that from colliding.
     */
    @Column(nullable = false, length = 50)
    private String scope;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * A short hash/summary of the request's meaningful content (e.g.
     * the order id being paid for). If the SAME key arrives again but
     * with a DIFFERENT fingerprint, that's a client bug (key reuse
     * across unrelated requests), not a legitimate retry — rejected
     * rather than silently replaying the wrong cached response.
     */
    @Column(name = "request_fingerprint", nullable = false, length = 255)
    private String requestFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IdempotencyKeyStatus status;

    /** The serialized (JSON) response to replay on a duplicate request — populated once status becomes COMPLETED. */
    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
