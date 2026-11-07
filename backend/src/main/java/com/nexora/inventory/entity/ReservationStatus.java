package com.nexora.inventory.entity;

/**
 * Lifecycle states of an Inventory Reservation in the Two-Phase Commit Engine.
 *
 * RESERVED:   Stock is locked for 10 minutes awaiting customer payment completion.
 * COMMITTED:  Payment is cryptographically verified; stock is permanently consumed.
 * RELEASED:   Customer cancelled order or payment failed; stock returned to available inventory.
 * EXPIRED:    10-minute TTL elapsed without payment; auto-released by reservation scheduler.
 */
public enum ReservationStatus {
    RESERVED,
    COMMITTED,
    RELEASED,
    EXPIRED
}
