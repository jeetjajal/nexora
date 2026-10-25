package com.nexora.concurrency.dto;

import com.nexora.concurrency.model.ConcurrencyStrategy;
import jakarta.validation.constraints.Max;
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
public class ConcurrencySimulationRequest {

    @NotNull(message = "Product ID cannot be null")
    private Long productId;

    @Min(value = 1, message = "Initial stock must be at least 1")
    @Builder.Default
    private int initialStock = 1;

    @Min(value = 2, message = "Total concurrent threads must be at least 2")
    @Max(value = 200, message = "Total concurrent threads capped at 200 for benchmark safety")
    @Builder.Default
    private int totalThreads = 100;

    @Builder.Default
    private ConcurrencyStrategy strategy = ConcurrencyStrategy.PESSIMISTIC_LOCK;
}
