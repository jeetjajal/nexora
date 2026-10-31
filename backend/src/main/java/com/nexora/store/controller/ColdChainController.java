package com.nexora.store.controller;

import com.nexora.auth.security.UserPrincipal;
import com.nexora.common.ApiResponse;
import com.nexora.store.dto.*;
import com.nexora.store.service.ColdChainAndExpiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/store")
@RequiredArgsConstructor
public class ColdChainController {

    private final ColdChainAndExpiryService coldChainService;

    @GetMapping("/{storeId}/coldchain/batches")
    @PreAuthorize("hasAnyRole('STORE_MANAGER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<InventoryBatchResponse>>> getStoreBatches(
            @PathVariable Long storeId,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<InventoryBatchResponse> batches = coldChainService.getStoreBatches(storeId);
        return ResponseEntity.ok(ApiResponse.success("Store inventory batches retrieved", batches));
    }

    @PostMapping("/{storeId}/coldchain/batches")
    @PreAuthorize("hasAnyRole('STORE_MANAGER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<InventoryBatchResponse>> createBatch(
            @PathVariable Long storeId,
            @Valid @RequestBody CreateInventoryBatchRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {

        req.setStoreId(storeId);
        InventoryBatchResponse response = coldChainService.createBatch(req);
        return ResponseEntity.ok(ApiResponse.success("Inventory batch created with FEFO priority", response));
    }

    @PostMapping("/coldchain/batches/{batchId}/quarantine")
    @PreAuthorize("hasAnyRole('STORE_MANAGER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<InventoryBatchResponse>> toggleQuarantine(
            @PathVariable Long batchId,
            @RequestParam boolean quarantined,
            @AuthenticationPrincipal UserPrincipal principal) {

        InventoryBatchResponse response = coldChainService.toggleQuarantine(batchId, quarantined);
        return ResponseEntity.ok(ApiResponse.success("Batch quarantine status updated", response));
    }

    @PostMapping("/coldchain/telemetry")
    public ResponseEntity<ApiResponse<TemperatureTelemetryResponse>> recordTelemetry(
            @Valid @RequestBody TemperatureTelemetryRequest req) {

        TemperatureTelemetryResponse response = coldChainService.recordTemperatureTelemetry(req);
        return ResponseEntity.ok(ApiResponse.success("IoT temperature telemetry recorded", response));
    }

    @GetMapping("/{storeId}/coldchain/overview")
    @PreAuthorize("hasAnyRole('STORE_MANAGER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ColdChainOverviewResponse>> getOverview(
            @PathVariable Long storeId,
            @AuthenticationPrincipal UserPrincipal principal) {

        ColdChainOverviewResponse overview = coldChainService.getColdChainOverview(storeId);
        return ResponseEntity.ok(ApiResponse.success("Cold chain telemetry overview retrieved", overview));
    }
}
