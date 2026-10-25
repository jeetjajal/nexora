package com.nexora.payment.exception;

/**
 * Thrown when a payment operation is attempted against an order that
 * isn't in the right state for it — e.g. trying to initiate payment
 * for an order that's already PAID, or that's been CANCELLED. Returned
 * as 409 CONFLICT.
 */
public class InvalidPaymentStateException extends RuntimeException {

    public InvalidPaymentStateException(String message) {
        super(message);
    }
}
