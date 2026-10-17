package com.nexora.inventory.mapper;

import com.nexora.inventory.dto.InventoryResponse;
import com.nexora.inventory.entity.Inventory;
import com.nexora.inventory.entity.InventoryStatus;

public class InventoryMapper {

    private InventoryMapper() {
    }

    public static InventoryResponse toResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .productId(inventory.getProduct().getId())
                .quantity(inventory.getQuantity())
                .lowStockThreshold(inventory.getLowStockThreshold())
                .status(computeStatus(inventory).name())
                .build();
    }

    /**
     * PHASE 6: derive OUT_OF_STOCK / LOW_STOCK / IN_STOCK purely from
     * the current quantity vs. this product's configured threshold —
     * never stored, always computed fresh so it can't drift out of
     * sync with the real quantity.
     */
    private static InventoryStatus computeStatus(Inventory inventory) {
        int quantity = inventory.getQuantity();
        int threshold = inventory.getLowStockThreshold();

        if (quantity <= 0) {
            return InventoryStatus.OUT_OF_STOCK;
        }
        if (quantity <= threshold) {
            return InventoryStatus.LOW_STOCK;
        }
        return InventoryStatus.IN_STOCK;
    }
}
