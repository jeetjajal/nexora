package com.nexora.payment.entity;

/**
 * What kind of event a PaymentTransaction row records. Only the first
 * two are used in Phase 9 — WEBHOOK_* values are deliberately NOT
 * defined yet; Phase 10 (signature verification & webhooks) will add
 * them when that logic actually exists, rather than scaffolding ahead
 * of what this phase implements.
 */
public enum PaymentEventType {
    /** A Razorpay order was successfully created for a Nexora order. */
    ORDER_CREATED,

    /**
     * The frontend reported a payment_id/signature back to us after
     * the Razorpay Checkout widget closed. Recorded for audit only —
     * see PaymentService's javadoc for why this can NEVER be trusted
     * to mark a payment successful on its own.
     */
    CLIENT_REPORTED_RESULT
}
