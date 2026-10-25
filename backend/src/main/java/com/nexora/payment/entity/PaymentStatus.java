package com.nexora.payment.entity;

/**
 * The 5 payment statuses from the roadmap. Distinct from OrderStatus
 * (Phase 8) — a Payment tracks the money side of things; an Order
 * tracks the fulfillment side. They're related but not identical:
 * an order can be CANCELLED after its payment already SUCCEEDED
 * (triggering a REFUND, Phase 11), for example.
 */
public enum PaymentStatus {
    CREATED,
    PENDING,
    SUCCESS,
    FAILED,
    REFUNDED
}
