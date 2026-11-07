package com.nexora.payment.entity;

/**
 * Audit event types recorded in the immutable PaymentTransaction ledger.
 */
public enum PaymentEventType {
    /** A Razorpay order was successfully created for a Nexora order. */
    ORDER_CREATED,

    /** The frontend reported a payment_id/signature back to us (historical). */
    CLIENT_REPORTED_RESULT,

    /** The client-callback signature was verified and matched — payment confirmed. */
    CLIENT_SIGNATURE_VERIFIED,

    /** The client-callback signature did NOT match — rejected, payment status unchanged. */
    CLIENT_SIGNATURE_INVALID,

    /** A webhook's signature was valid and reported the payment as captured. */
    WEBHOOK_PAYMENT_CAPTURED,

    /** A webhook's signature was valid and reported the payment as failed. */
    WEBHOOK_PAYMENT_FAILED,

    /** A webhook arrived with an invalid HMAC signature — rejected outright. */
    WEBHOOK_SIGNATURE_INVALID,

    /** A validly-signed webhook reported an event already processed — logged, not reprocessed. */
    WEBHOOK_DUPLICATE_IGNORED,

    /** Asynchronous webhook confirmed a refund was processed by the gateway. */
    WEBHOOK_REFUND_PROCESSED,

    /** Asynchronous webhook reported a refund failed at the gateway or issuing bank. */
    WEBHOOK_REFUND_FAILED,

    /** A partial refund was authorized and successfully executed. */
    PARTIAL_REFUND_PROCESSED,

    /** A SUCCESS payment was refunded via the gateway's refund API. */
    REFUND_COMPLETED
}
