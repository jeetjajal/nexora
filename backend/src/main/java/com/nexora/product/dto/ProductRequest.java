package com.nexora.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Used for BOTH create and update. `initialStock` is only meaningful
 * on CREATE (it seeds the paired Inventory row — see
 * ProductService.createProduct) and is simply ignored on update;
 * stock changes after creation go through InventoryController
 * instead, which is a deliberately separate, smaller, more frequent
 * operation than a full product edit.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 150, message = "Product name must be between 2 and 150 characters")
    private String name;

    @Size(max = 1000, message = "Description must be under 1000 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price cannot be negative")
    private BigDecimal price;

    @DecimalMin(value = "0.0", inclusive = true, message = "Discount cannot be negative")
    private BigDecimal discount;

    private String imageUrl;

    /**
     * Category names (must already exist — see CategoryController).
     */
    private Set<String> categoryNames;

    /**
     * Starting stock quantity, used only when creating a new product.
     * Defaults to 0 (out of stock) if omitted.
     */
    @Min(value = 0, message = "Initial stock cannot be negative")
    private Integer initialStock;
}
