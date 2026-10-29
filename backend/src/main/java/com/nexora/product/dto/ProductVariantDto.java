package com.nexora.product.dto;

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
public class ProductVariantDto {
    private Long id;
    private Long productId;
    private String sku;
    private String name;
    private String unit;
    private String packSize;
    private BigDecimal mrp;
    private BigDecimal price;
    private BigDecimal discount;
    private String barcode;
    private boolean active;
}
