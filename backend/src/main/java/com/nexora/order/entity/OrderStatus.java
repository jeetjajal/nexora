package com.nexora.order.entity;

/**
 * The full order lifecycle, exactly as specified in the roadmap.
 * Not every status is reachable from every other status — see
 * OrderService.ALLOWED_TRANSITIONS for the actual state machine that
 * enforces this ("prevent invalid state transitions").
 */
public enum OrderStatus {
    CREATED,
    PAYMENT_PENDING,
    PAID,
    CONFIRMED,
    PREPARING,
    READY,
    RIDER_ASSIGNED,
    PICKED_UP,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED,
    REJECTED
}
