package com.nexora.order.exception;

/**
 * Thrown when checkout is attempted with no items in the cart.
 * Returned as 400 BAD_REQUEST — there's nothing wrong with the
 * request's shape, but there's nothing meaningful for it to do.
 */
public class EmptyCartException extends RuntimeException {

    public EmptyCartException() {
        super("Cannot checkout: your cart is empty");
    }
}
