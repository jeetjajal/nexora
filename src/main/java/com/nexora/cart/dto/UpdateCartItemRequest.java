package com.nexora.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Sets a cart item's quantity to an ABSOLUTE value (not a +/- delta) —
 * same convention InventoryUpdateRequest used in Phase 4/6. To
 * "increase by 1" the client sends currentQuantity + 1; to remove the
 * item entirely, use DELETE /api/v1/cart/items/{id} instead of setting
 * quantity to 0 (0 isn't a valid quantity here — see @Min(1)).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCartItemRequest {

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1 — use DELETE to remove an item")
    private Integer quantity;
}
