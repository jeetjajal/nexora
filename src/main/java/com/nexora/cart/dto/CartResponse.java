package com.nexora.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * `subtotal` is the sum of every line item's lineTotal — nothing more.
 * Delivery fee, coupon discounts, and the final payable total are
 * checkout-time concerns that belong to Order (Phase 8), not Cart.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {
    private Long id;
    private List<CartItemResponse> items;
    private Integer totalItemCount; // sum of all quantities
    private BigDecimal subtotal;
}
