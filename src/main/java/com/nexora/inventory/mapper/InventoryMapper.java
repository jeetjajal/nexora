package com.nexora.inventory.mapper;

import com.nexora.inventory.dto.InventoryResponse;
import com.nexora.inventory.entity.Inventory;

public class InventoryMapper {

    private InventoryMapper() {
    }

    public static InventoryResponse toResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .productId(inventory.getProduct().getId())
                .quantity(inventory.getQuantity())
                .build();
    }
}
