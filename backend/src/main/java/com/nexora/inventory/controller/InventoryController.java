package com.nexora.inventory.controller;

import com.nexora.auth.security.UserPrincipal;
import com.nexora.common.ApiResponse;
import com.nexora.inventory.dto.InventoryResponse;
import com.nexora.inventory.dto.InventoryUpdateRequest;
import com.nexora.inventory.dto.LowStockThresholdRequest;
import com.nexora.inventory.dto.StockAdjustmentRequest;
import com.nexora.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products/{productId}/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * Any authenticated user can check a product's current stock —
     * PHASE 6: the response now also includes `status`
     * (IN_STOCK / LOW_STOCK / OUT_OF_STOCK) and `lowStockThreshold`,
     * useful for a customer's product page to show a stock badge, or
     * a store owner's dashboard to flag items needing restock.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventory(@PathVariable Long productId) {
        InventoryResponse response = inventoryService.getInventoryForProduct(productId);
        return ResponseEntity.ok(ApiResponse.success("Inventory fetched successfully", response));
    }

    /**
     * PHASE 4: sets stock to an absolute quantity — e.g. correcting a
     * count after a manual shelf audit. Only the owning STORE_OWNER or
     * an ADMIN may do this.
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

    /**
     * PHASE 6: restock — adds `amount` to the current quantity.
     *
     * Sample request: { "amount": 20 }
     */
    @PostMapping("/increase")
    @PreAuthorize("hasAnyRole('STORE_OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<InventoryResponse>> increaseStock(
            @PathVariable Long productId,
            @Valid @RequestBody StockAdjustmentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        InventoryResponse response = inventoryService.increaseStock(productId, request, principal);
        return ResponseEntity.ok(ApiResponse.success("Stock increased successfully", response));
    }

    /**
     * PHASE 6: sell/consume stock — subtracts `amount` from the
     * current quantity, but ONLY if enough is available. This is the
     * concurrency-safe operation (see InventoryRepository for the full
     * explanation) — under simultaneous requests for the last unit(s)
     * of a product, exactly one succeeds and the rest fail cleanly
     * with 409 INSUFFICIENT_STOCK, never allowing quantity to go negative.
     *
     * Sample request: { "amount": 1 }
     */
    @PostMapping("/reduce")
    @PreAuthorize("hasAnyRole('STORE_OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<InventoryResponse>> reduceStock(
            @PathVariable Long productId,
            @Valid @RequestBody StockAdjustmentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        InventoryResponse response = inventoryService.reduceStock(productId, request, principal);
        return ResponseEntity.ok(ApiResponse.success("Stock reduced successfully", response));
    }

    /**
     * PHASE 6: configure per-product low-stock sensitivity.
     *
     * Sample request: { "threshold": 10 }
     */
    @PutMapping("/threshold")
    @PreAuthorize("hasAnyRole('STORE_OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<InventoryResponse>> updateLowStockThreshold(
            @PathVariable Long productId,
            @Valid @RequestBody LowStockThresholdRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        InventoryResponse response = inventoryService.updateLowStockThreshold(productId, request, principal);
        return ResponseEntity.ok(ApiResponse.success("Low-stock threshold updated successfully", response));
    }
}
