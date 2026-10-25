package com.nexora.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PHASE 6 ADDITION: `status` and `lowStockThreshold` — everything else
 * is unchanged from Phase 4. `status` is a derived value (see
 * InventoryMapper), not a raw database column.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryResponse {
    private Long id;
    private Long productId;
    private Integer quantity;
    private Integer lowStockThreshold;
    private String status; // OUT_OF_STOCK | LOW_STOCK | IN_STOCK
}
