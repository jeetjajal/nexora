package com.nexora.inventory.exception;

/**
 * Thrown when a stock-reduce request asks for more units than are
 * currently available. This is the "clean failure" outcome of the
 * atomic conditional UPDATE in InventoryRepository.reduceStockIfAvailable
 * — see that method's javadoc for the full race-condition explanation.
 * Returned as 409 CONFLICT: the request itself was well-formed, but it
 * conflicts with the current state of the resource (not enough stock
 * right now).
 */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(Long productId, int requestedAmount, int availableQuantity) {
        super("Insufficient stock for product id " + productId + ": requested " + requestedAmount +
                " but only " + availableQuantity + " available");
    }
}
