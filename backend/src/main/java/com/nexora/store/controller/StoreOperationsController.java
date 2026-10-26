package com.nexora.store.controller;

import com.nexora.auth.security.UserPrincipal;
import com.nexora.common.ApiResponse;
import com.nexora.order.dto.OrderItemResponse;
import com.nexora.order.dto.OrderResponse;
import com.nexora.store.dto.*;
import com.nexora.store.service.StoreOperationsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class StoreOperationsController {

    private final StoreOperationsService storeOperationsService;

    @GetMapping("/api/v1/stores/{storeId}/operations/queue")
    @PreAuthorize("hasAnyRole('STORE_OWNER','STORE_MANAGER','PICKER','PACKER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<StoreQueueMetricsResponse>> getStoreQueue(
            @PathVariable Long storeId,
            @AuthenticationPrincipal UserPrincipal principal) {

        StoreQueueMetricsResponse response = storeOperationsService.getStoreQueue(principal, storeId);
        return ResponseEntity.ok(ApiResponse.success("Store queue retrieved successfully", response));
    }

    @GetMapping("/api/v1/stores/{storeId}/inventory")
    @PreAuthorize("hasAnyRole('STORE_OWNER','STORE_MANAGER','PICKER','PACKER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<StoreInventoryItemResponse>>> getStoreInventory(
            @PathVariable Long storeId,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<StoreInventoryItemResponse> response = storeOperationsService.getStoreInventory(principal, storeId);
        return ResponseEntity.ok(ApiResponse.success("Store inventory retrieved successfully", response));
    }

    @PostMapping("/api/v1/stores/{storeId}/inventory/adjust")
    @PreAuthorize("hasAnyRole('STORE_OWNER','STORE_MANAGER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<StoreInventoryItemResponse>> adjustStock(
            @PathVariable Long storeId,
            @Valid @RequestBody StoreStockAdjustmentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        StoreInventoryItemResponse response = storeOperationsService.adjustStock(principal, storeId, request);
        return ResponseEntity.ok(ApiResponse.success("Stock adjusted successfully", response));
    }

    @PostMapping("/api/v1/orders/{orderId}/picking/start")
    @PreAuthorize("hasAnyRole('PICKER','STORE_MANAGER','STORE_OWNER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> startPicking(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal) {

        OrderResponse response = storeOperationsService.startPicking(principal, orderId);
        return ResponseEntity.ok(ApiResponse.success("Picking started for order #" + orderId, response));
    }

    @PostMapping("/api/v1/orders/{orderId}/items/{itemId}/pick")
    @PreAuthorize("hasAnyRole('PICKER','STORE_MANAGER','STORE_OWNER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<OrderItemResponse>> pickItem(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @RequestBody(required = false) PickItemRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        OrderItemResponse response = storeOperationsService.pickItem(principal, orderId, itemId, request);
        return ResponseEntity.ok(ApiResponse.success("Item pick state updated", response));
    }

    @PostMapping("/api/v1/orders/{orderId}/picking/complete")
    @PreAuthorize("hasAnyRole('PICKER','STORE_MANAGER','STORE_OWNER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> completePicking(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal) {

        OrderResponse response = storeOperationsService.completePicking(principal, orderId);
        return ResponseEntity.ok(ApiResponse.success("Picking completed for order #" + orderId, response));
    }

    @PostMapping("/api/v1/orders/{orderId}/packing/complete")
    @PreAuthorize("hasAnyRole('PACKER','STORE_MANAGER','STORE_OWNER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> completePacking(
            @PathVariable Long orderId,
            @Valid @RequestBody CompletePackingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        OrderResponse response = storeOperationsService.completePacking(principal, orderId, request);
        return ResponseEntity.ok(ApiResponse.success("Packing completed. Order is READY for dispatch.", response));
    }
}
