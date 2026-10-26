package com.nexora.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceOverrideRequest {
    @NotNull(message = "Override selling price is required")
    @DecimalMin(value = "1.00", message = "Price must be at least ₹1.00")
    private BigDecimal overridePrice;

    private BigDecimal overrideDiscount;

    @NotBlank(message = "Audit reason is required for price override")
    private String reason;
}
