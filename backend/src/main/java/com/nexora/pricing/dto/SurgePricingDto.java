package com.nexora.pricing.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurgePricingDto {
    private Long id;
    @NotNull(message = "Hub ID is required")
    private Long hubId;
    private String hubName;
    private String zoneName;
    @NotNull(message = "isActive flag is required")
    private Boolean isActive;

    @NotNull(message = "Multiplier is required")
    @DecimalMin(value = "1.00", message = "Multiplier cannot be less than 1.00")
    @DecimalMax(value = "2.50", message = "Multiplier cannot exceed safety cap of 2.50 to prevent surge gouging")
    private BigDecimal multiplier;

    @NotNull(message = "Extra flat fee is required")
    @DecimalMin(value = "0.00", message = "Flat fee cannot be negative")
    @DecimalMax(value = "100.00", message = "Extra flat fee cannot exceed ₹100")
    private BigDecimal extraFlatFee;

    private String reason;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
