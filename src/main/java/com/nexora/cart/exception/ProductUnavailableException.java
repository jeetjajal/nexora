package com.nexora.cart.exception;

/**
 * Thrown when trying to add/increase a product in a cart that's been
 * marked unavailable (Product.available = false — e.g. the store owner
 * deactivated it, Phase 4/5). Returned as 409 CONFLICT: the product id
 * is valid and exists, but it can't currently be added to a cart.
 */
public class ProductUnavailableException extends RuntimeException {

    public ProductUnavailableException(Long productId) {
        super("Product " + productId + " is not currently available");
    }
}
