package com.nexora.microservice.model;

/**
 * Status of a distributed Saga workflow across microservices.
 */
public enum SagaStatus {
    STARTED,
    ORDER_PENDING,
    INVENTORY_RESERVED,
    PAYMENT_CAPTURED,
    NOTIFICATION_SENT,
    COMPLETED_SUCCESSFULLY,
    PAYMENT_FAILED,
    COMPENSATING_INVENTORY_RELEASED,
    COMPENSATING_ORDER_CANCELLED,
    ROLLED_BACK_FAILED
}
