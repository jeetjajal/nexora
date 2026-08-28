package com.nexora.payment.entity;

/**
 * What kind of event a PaymentTransaction row records.
 *
 * ORDER_CREATED and CLIENT_REPORTED_RESULT are from Phase 9.
 * Everything else is PHASE 10: real signature verification now exists,
 * so the client-callback path can be cryptographically confirmed
 * (CLIENT_SIGNATURE_VERIFIED / CLIENT_SIGNATURE_INVALID), and real
 * webhook handling exists (WEBHOOK_PAYMENT_CAPTURED /
 * WEBHOOK_PAYMENT_FAILED / WEBHOOK_SIGNATURE_INVALID /
 * WEBHOOK_DUPLICATE_IGNORED).
 */
public enum PaymentEventType {
    /** A Razorpay order was successfully created for a Nexora order. */
    ORDER_CREATED,

    /**
     * PHASE 9: the frontend reported a payment_id/signature back to us
     * after the Checkout widget closed, recorded before Phase 10's
     * signature verification existed. Kept for historical audit
     * entries; Phase 10 now logs CLIENT_SIGNATURE_VERIFIED/INVALID
     * instead once a signature is actually checked.
     */
    CLIENT_REPORTED_RESULT,

    /** PHASE 10: the client-callback signature was verified and matched — payment confirmed via this path. */
    CLIENT_SIGNATURE_VERIFIED,

    /** PHASE 10: the client-callback signature did NOT match — rejected, payment status unchanged. */
    CLIENT_SIGNATURE_INVALID,

    /** PHASE 10: a webhook's signature was valid and reported the payment as captured (successful). */
    WEBHOOK_PAYMENT_CAPTURED,

    /** PHASE 10: a webhook's signature was valid and reported the payment as failed. */
    WEBHOOK_PAYMENT_FAILED,

    /** PHASE 10: a webhook arrived with a signature that did NOT match — rejected outright, nothing else about it is trusted. */
    WEBHOOK_SIGNATURE_INVALID,

    /** PHASE 10: a validly-signed webhook reported an event we've already processed — logged, not reprocessed. */
    WEBHOOK_DUPLICATE_IGNORED
}
