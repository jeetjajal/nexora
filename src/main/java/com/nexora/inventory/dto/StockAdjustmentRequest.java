package com.nexora.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Used for BOTH increase and reduce — the amount is always a positive
 * delta; which direction it moves stock is determined by which
 * endpoint receives it (POST .../increase vs POST .../reduce), not by
 * the sign of the number. This keeps the request shape simple and
 * avoids a "negative amount to mean reduce" convention, which would be
 * easy to misuse.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustmentRequest {

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be at least 1")
    private Integer amount;
}
