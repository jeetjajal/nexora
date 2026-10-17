package com.nexora.order.exception;

import com.nexora.order.entity.OrderStatus;

/**
 * Thrown when an order status change is requested that the state
 * machine doesn't allow (e.g. DELIVERED -> PREPARING). Returned as
 * 409 CONFLICT: the request is well-formed, but conflicts with the
 * order's current state.
 */
public class InvalidOrderStatusTransitionException extends RuntimeException {

    public InvalidOrderStatusTransitionException(OrderStatus from, OrderStatus to) {
        super("Cannot transition order from " + from + " to " + to);
    }
}
