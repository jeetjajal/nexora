package com.nexora.category.exception;

/**
 * Thrown when an ADMIN tries to delete a category that's still
 * attached to at least one store or product. Returned as 409 CONFLICT
 * — the category itself is fine, but the delete can't proceed while
 * something still references it. The client should detach the
 * category from those stores/products first (or leave it in place).
 */
public class CategoryInUseException extends RuntimeException {

    public CategoryInUseException(String categoryName) {
        super("Cannot delete category '" + categoryName +
                "': it is still attached to one or more stores or products");
    }
}
