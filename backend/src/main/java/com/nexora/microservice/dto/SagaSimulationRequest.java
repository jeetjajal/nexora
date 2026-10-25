package com.nexora.microservice.dto;

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
public class SagaSimulationRequest {

    @NotNull(message = "Customer ID cannot be null")
    @Builder.Default
    private Long customerId = 1L;

    @NotNull(message = "Product ID cannot be null")
    @Builder.Default
    private Long productId = 1L;

    @Min(value = 1, message = "Quantity must be at least 1")
    @Builder.Default
    private int quantity = 1;

    @Min(value = 1, message = "Amount must be at least 1")
    @Builder.Default
    private double amount = 299.0;

    /**
     * If true, payment gateway simulation fails to demonstrate compensating transaction rollback!
     */
    @Builder.Default
    private boolean simulatePaymentFailure = false;
}
