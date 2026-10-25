package com.nexora.product.exception;

/**
 * Thrown for product data that passes basic bean validation
 * (@NotNull, @DecimalMin, etc. on ProductRequest) but is still
 * logically inconsistent — e.g. a discount larger than the price
 * itself. Bean validation only checks each field in isolation; a rule
 * that relates TWO fields to each other belongs in the service layer.
 * Returned as 400 BAD_REQUEST, same as a bean-validation failure,
 * since from the client's point of view it's the same category of
 * problem: "the data you sent doesn't make sense."
 */
public class InvalidProductDataException extends RuntimeException {

    public InvalidProductDataException(String message) {
        super(message);
    }
}
