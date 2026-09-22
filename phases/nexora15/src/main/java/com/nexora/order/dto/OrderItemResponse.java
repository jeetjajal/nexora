package com.nexora.order.dto;

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
public class OrderItemResponse {
    private Long id;
    private Long productId;
    private String productName; // the SNAPSHOT, not the live product name
    private BigDecimal unitPrice; // the SNAPSHOT, not the live price
    private Integer quantity;
    private BigDecimal subtotal;
}
