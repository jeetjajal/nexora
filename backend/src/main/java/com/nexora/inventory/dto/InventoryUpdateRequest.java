package com.nexora.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Sets stock to an ABSOLUTE quantity (not a +/- delta). Phase 4 keeps
 * this simple and direct ("set stock to 25"); Phase 6 introduces
 * dedicated increase/reduce operations with proper transactional
 * guarantees, and Phase 21 adds concurrency control (optimistic
 * locking via @Version) for high-contention scenarios like flash sales.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InventoryUpdateRequest {

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative — inventory can never go below zero")
    private Integer quantity;
}
