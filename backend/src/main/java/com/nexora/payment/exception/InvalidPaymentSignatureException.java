package com.nexora.payment.exception;

/**
 * Thrown when a Razorpay signature (either the client-callback
 * payment signature or a webhook signature) fails verification. This
 * is a security-relevant rejection, not a routine validation error —
 * it means either the data was tampered with, or it didn't genuinely
 * originate from Razorpay at all. Returned as 400 BAD_REQUEST.
 */
public class InvalidPaymentSignatureException extends RuntimeException {

    public InvalidPaymentSignatureException(String message) {
        super(message);
    }
}
