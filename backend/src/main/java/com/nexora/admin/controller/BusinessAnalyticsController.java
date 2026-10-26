package com.nexora.admin.controller;

import com.nexora.admin.dto.analytics.AnalyticsOverviewResponse;
import com.nexora.admin.service.BusinessAnalyticsService;
import com.nexora.auth.security.UserPrincipal;
import com.nexora.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
public class BusinessAnalyticsController {

    private final BusinessAnalyticsService analyticsService;

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AnalyticsOverviewResponse>> getOverview(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long storeId,
            @AuthenticationPrincipal UserPrincipal principal) {

        AnalyticsOverviewResponse response = analyticsService.getOverview(principal, startDate, endDate, storeId);
        return ResponseEntity.ok(ApiResponse.success("Analytics overview calculated successfully", response));
    }
}
