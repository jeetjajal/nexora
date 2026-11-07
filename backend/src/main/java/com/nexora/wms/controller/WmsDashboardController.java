package com.nexora.wms.controller;

import com.nexora.common.ApiResponse;
import com.nexora.wms.dto.WmsDashboardMetricsResponse;
import com.nexora.wms.service.WmsFulfillmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * WMS DARK STORE DASHBOARD CONTROLLER (NEXORA 3.0 WS1 - WS6)
 */
@RestController
@RequestMapping("/api/v1/wms/stores")
@RequiredArgsConstructor
public class WmsDashboardController {

    private final WmsFulfillmentService wmsFulfillmentService;

    @GetMapping("/{storeId}/dashboard/metrics")
    @PreAuthorize("hasAnyRole('STORE_MANAGER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<WmsDashboardMetricsResponse>> getDashboardMetrics(@PathVariable Long storeId) {
        WmsDashboardMetricsResponse metrics = wmsFulfillmentService.getWmsDashboardMetrics(storeId);
        return ResponseEntity.ok(ApiResponse.success("WMS dashboard metrics retrieved successfully", metrics));
    }
}
