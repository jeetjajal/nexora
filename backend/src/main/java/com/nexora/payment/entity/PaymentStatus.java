package com.nexora.payment.entity;

/**
 * Payment statuses across the order and transaction lifecycle.
 * Distinct from OrderStatus — Payment tracks the financial settlement.
 */
public enum PaymentStatus {
    CREATED,
    PENDING,
    SUCCESS,
    FAILED,
    PARTIALLY_REFUNDED,
    REFUNDED
}
