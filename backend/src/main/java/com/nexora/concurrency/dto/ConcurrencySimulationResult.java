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
public class ConcurrencySimulationResult {

    private Long productId;
    private ConcurrencyStrategy strategy;
    private int initialStock;
    private int finalStock;
    private int totalRequests;
    private int successfulPurchases;
    private int failedPurchases;
    private long totalDurationMs;
    private double throughputReqPerSec;
    private boolean oversellingPrevented;
    private String explanation;
}
