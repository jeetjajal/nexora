package com.nexora.concurrency.dto;

import com.nexora.concurrency.model.ConcurrencyStrategy;
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
public class FlashSalePurchaseResponse {

    private boolean success;
    private Long productId;
    private Integer remainingStock;
    private ConcurrencyStrategy strategyUsed;
    private long executionTimeMs;
    private String message;
}
