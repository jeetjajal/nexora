package com.nexora.product.exception;

/**
 * Thrown when trying to hard-delete (Phase 4's DELETE endpoint) a
 * product that's referenced by at least one real order's OrderItem
 * (Phase 8). Deleting it would either orphan historical order records
 * or fail with a raw foreign-key violation at the database level —
 * this gives a clean 409 CONFLICT instead, and points to the
 * deactivate endpoint as the right tool once a product has real order
 * history.
 */
public class ProductInUseException extends RuntimeException {

    public ProductInUseException(Long productId) {
        super("Cannot delete product " + productId +
                ": it is referenced by at least one existing order. Use deactivate instead.");
    }
}
