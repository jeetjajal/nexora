package com.nexora.dispute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDisputeRequest {
    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotBlank(message = "Dispute reason is required")
    private String reason;

    private String customerNotes;

    @NotNull(message = "Claim amount is required")
    private BigDecimal claimAmount;
}
