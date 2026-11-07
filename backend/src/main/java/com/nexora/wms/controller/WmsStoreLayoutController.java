package com.nexora.wms.controller;

import com.nexora.common.ApiResponse;
import com.nexora.wms.dto.StoreLayoutResponse;
import com.nexora.wms.entity.ProductStorageLocation;
import com.nexora.wms.service.StoreLayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * WMS STORE LAYOUT CONTROLLER (NEXORA 3.0 WS1)
 */
@RestController
@RequestMapping("/api/v1/wms/stores")
@RequiredArgsConstructor
public class WmsStoreLayoutController {

    private final StoreLayoutService storeLayoutService;

    @GetMapping("/{storeId}/layout")
    @PreAuthorize("hasAnyRole('STORE_MANAGER','PICKER','PACKER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<StoreLayoutResponse>> getStoreLayout(@PathVariable Long storeId) {
        StoreLayoutResponse layout = storeLayoutService.getStoreLayout(storeId);
        return ResponseEntity.ok(ApiResponse.success("Store layout retrieved successfully", layout));
    }

    @PostMapping("/{storeId}/products/{productId}/bin")
    @PreAuthorize("hasAnyRole('STORE_MANAGER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ProductStorageLocation>> assignProductToBin(
            @PathVariable Long storeId,
            @PathVariable Long productId,
            @RequestParam Long binId,
            @RequestParam(defaultValue = "true") boolean isPrimary) {
        ProductStorageLocation location = storeLayoutService.assignProductToBin(storeId, productId, binId, isPrimary);
        return ResponseEntity.ok(ApiResponse.success("Product storage location assigned successfully", location));
    }
}
