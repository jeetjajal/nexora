package com.nexora.store.dto;

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
public class StoreInventoryItemResponse {
    private Long productId;
    private String productName;
    private String sku;
    private String unit;
    private BigDecimal price;
    private Integer quantity;
    private Integer lowStockThreshold;
    private String status; // IN_STOCK, LOW_STOCK, OUT_OF_STOCK
}
