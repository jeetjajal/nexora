package com.nexora.admin.controller;

import com.nexora.auth.security.UserPrincipal;
import com.nexora.common.ApiResponse;
import com.nexora.delivery.dto.AssignBatchRequest;
import com.nexora.delivery.dto.BatchGenerationRequest;
import com.nexora.delivery.dto.DeliveryBatchResponse;
import com.nexora.delivery.service.DeliveryBatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/batches")
@RequiredArgsConstructor
public class AdminDeliveryBatchController {

    private final DeliveryBatchingService batchingService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','STORE_MANAGER')")
    public ResponseEntity<ApiResponse<List<DeliveryBatchResponse>>> listBatches(
            @RequestParam(required = false) Long storeId,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<DeliveryBatchResponse> batches = (storeId != null) 
                ? batchingService.getBatchesByStore(storeId) 
                : batchingService.getAllBatches();

        return ResponseEntity.ok(ApiResponse.success("Batches retrieved successfully", batches));
    }

    @GetMapping("/{batchId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','STORE_MANAGER')")
    public ResponseEntity<ApiResponse<DeliveryBatchResponse>> getBatch(
            @PathVariable Long batchId,
            @AuthenticationPrincipal UserPrincipal principal) {

        DeliveryBatchResponse batch = batchingService.getBatchById(batchId);
        return ResponseEntity.ok(ApiResponse.success("Batch details retrieved", batch));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','STORE_MANAGER')")
    public ResponseEntity<ApiResponse<List<DeliveryBatchResponse>>> generateBatches(
            @RequestBody(required = false) BatchGenerationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long targetStore = request != null ? request.getStoreId() : null;
        Integer maxSize = request != null ? request.getMaxBatchSize() : 3;
        Double maxProx = request != null ? request.getMaxProximityKm() : 2.5;

        List<DeliveryBatchResponse> generated = batchingService.generateBatchesForStore(targetStore, maxSize, maxProx);
        return ResponseEntity.ok(ApiResponse.success("Generated " + generated.size() + " delivery batches", generated));
    }

    @PostMapping("/{batchId}/assign")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','STORE_MANAGER')")
    public ResponseEntity<ApiResponse<DeliveryBatchResponse>> assignBatch(
            @PathVariable Long batchId,
            @RequestBody AssignBatchRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        DeliveryBatchResponse updated = batchingService.assignBatchToRider(batchId, request.getDeliveryPartnerId());
        return ResponseEntity.ok(ApiResponse.success("Batch successfully assigned to rider", updated));
    }

    @PostMapping("/{batchId}/dispatch")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','STORE_MANAGER')")
    public ResponseEntity<ApiResponse<DeliveryBatchResponse>> dispatchBatch(
            @PathVariable Long batchId,
            @AuthenticationPrincipal UserPrincipal principal) {

        DeliveryBatchResponse updated = batchingService.dispatchBatch(batchId);
        return ResponseEntity.ok(ApiResponse.success("Batch dispatched into transit", updated));
    }

    @PostMapping("/{batchId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','STORE_MANAGER')")
    public ResponseEntity<ApiResponse<DeliveryBatchResponse>> cancelBatch(
            @PathVariable Long batchId,
            @AuthenticationPrincipal UserPrincipal principal) {

        DeliveryBatchResponse updated = batchingService.cancelBatch(batchId);
        return ResponseEntity.ok(ApiResponse.success("Batch cancelled and unbundled", updated));
    }
}
