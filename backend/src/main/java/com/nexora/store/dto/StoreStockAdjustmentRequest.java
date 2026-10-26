package com.nexora.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreStockAdjustmentRequest {
    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Adjustment amount is required")
    private Integer amount;

    @NotBlank(message = "Action is required (ADD, SET, SUBTRACT)")
    private String action; // ADD, SET, SUBTRACT

    @NotBlank(message = "Audit reason is required for inventory safety")
    private String reason;
}
