package com.nexora.inventory.controller;

import com.nexora.auth.security.UserPrincipal;
import com.nexora.common.ApiResponse;
import com.nexora.inventory.dto.InventoryResponse;
import com.nexora.inventory.dto.InventoryUpdateRequest;
import com.nexora.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products/{productId}/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * Any authenticated user can check a product's current stock —
     * useful for a customer's product page to show "In Stock" /
     * "Out of Stock" without exposing the exact quantity if we didn't
     * want to (we do expose it here; the frontend decides how much of
     * this to display).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventory(@PathVariable Long productId) {
        InventoryResponse response = inventoryService.getInventoryForProduct(productId);
        return ResponseEntity.ok(ApiResponse.success("Inventory fetched successfully", response));
    }

    /**
     * Sets stock to an absolute quantity. Only the owning STORE_OWNER
     * or an ADMIN may do this — InventoryService verifies ownership
     * via the product's store.
     *
     * Sample request: { "quantity": 25 }
     */
    @PutMapping
    @PreAuthorize("hasAnyRole('STORE_OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<InventoryResponse>> updateStock(
            @PathVariable Long productId,
            @Valid @RequestBody InventoryUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        InventoryResponse response = inventoryService.updateStock(productId, request, principal);
        return ResponseEntity.ok(ApiResponse.success("Inventory updated successfully", response));
    }
}
