package com.nexora.wms.controller;

import com.nexora.auth.security.UserPrincipal;
import com.nexora.common.ApiResponse;
import com.nexora.wms.dto.CompletePackingWmsRequest;
import com.nexora.wms.dto.DispatchHandoverRequest;
import com.nexora.wms.dto.DispatchHandoverResponse;
import com.nexora.wms.entity.PackingTask;
import com.nexora.wms.service.WmsFulfillmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * WMS PACKING, QC & DISPATCH HANDOVER CONTROLLER (NEXORA 3.0 WS5)
 */
@RestController
@RequestMapping("/api/v1/wms")
@RequiredArgsConstructor
public class WmsPackingController {

    private final WmsFulfillmentService wmsFulfillmentService;

    @PostMapping("/orders/{orderId}/packing/complete")
    @PreAuthorize("hasAnyRole('STORE_MANAGER','PACKER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PackingTask>> completePacking(
            @PathVariable Long orderId,
            @Valid @RequestBody CompletePackingWmsRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Long userId = currentUser != null ? currentUser.getId() : 1L;
        PackingTask task = wmsFulfillmentService.completePacking(orderId, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Packing completed and bag sealed successfully", task));
    }

    @PostMapping("/orders/{orderId}/dispatch/handover")
    @PreAuthorize("hasAnyRole('STORE_MANAGER','PACKER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DispatchHandoverResponse>> handoverToDispatch(
            @PathVariable Long orderId,
            @Valid @RequestBody DispatchHandoverRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Long userId = currentUser != null ? currentUser.getId() : 1L;
        DispatchHandoverResponse response = wmsFulfillmentService.handoverToDispatch(orderId, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Order handed over to dispatch successfully", response));
    }
}
