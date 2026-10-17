package com.nexora.payment.gateway;

/**
 * Thrown when the Razorpay SDK call itself fails (network error,
 * invalid credentials, Razorpay-side error, etc.) — as opposed to a
 * successful call that reports a declined payment, which is a
 * perfectly normal outcome handled through PaymentStatus.FAILED
 * instead of an exception. Wrapped as its own type so
 * GlobalExceptionHandler can return a clean 502 BAD_GATEWAY rather
 * than an opaque 500 with a raw SDK stack trace.
 */
public class PaymentGatewayException extends RuntimeException {

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
