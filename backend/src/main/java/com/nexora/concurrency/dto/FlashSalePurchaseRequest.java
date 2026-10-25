package com.nexora.concurrency.dto;

import com.nexora.concurrency.model.ConcurrencyStrategy;
import jakarta.validation.constraints.Min;
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
public class FlashSalePurchaseRequest {

    @NotNull(message = "Product ID cannot be null")
    private Long productId;

    @Min(value = 1, message = "Quantity must be at least 1")
    @Builder.Default
    private int quantity = 1;

    @Builder.Default
    private ConcurrencyStrategy strategy = ConcurrencyStrategy.PESSIMISTIC_LOCK;
}
