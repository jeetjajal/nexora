package com.nexora.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productImageUrl;

    /** Product's current price minus its current discount — read live, every time. */
    private BigDecimal unitPrice;

    private Integer quantity;

    /** unitPrice * quantity */
    private BigDecimal lineTotal;

    private boolean productAvailable;
}
