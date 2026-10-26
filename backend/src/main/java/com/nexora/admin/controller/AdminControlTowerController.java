package com.nexora.admin.controller;

import com.nexora.admin.dto.*;
import com.nexora.admin.service.AdminControlTowerService;
import com.nexora.auth.security.UserPrincipal;
import com.nexora.common.ApiResponse;
import com.nexora.dispute.dto.CreateDisputeRequest;
import com.nexora.dispute.dto.DisputeDecisionRequest;
import com.nexora.dispute.dto.DisputeResponse;
import com.nexora.dispute.entity.DisputeStatus;
import com.nexora.pricing.dto.SurgePricingDto;
import com.nexora.product.dto.ProductResponse;
import com.nexora.product.entity.Product;
import com.nexora.product.mapper.ProductMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminControlTowerController {

    private final AdminControlTowerService adminService;

    @GetMapping("/control-tower")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ControlTowerOverviewResponse>> getControlTowerOverview(
            @AuthenticationPrincipal UserPrincipal principal) {

        ControlTowerOverviewResponse response = adminService.getControlTowerOverview(principal);
        return ResponseEntity.ok(ApiResponse.success("Control Tower metrics retrieved successfully", response));
    }

    @GetMapping("/fleet")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<FleetRiderStatusDto>>> getFleetStatus(
            @AuthenticationPrincipal UserPrincipal principal) {

        List<FleetRiderStatusDto> response = adminService.getFleetStatus(principal);
        return ResponseEntity.ok(ApiResponse.success("Fleet monitoring status retrieved successfully", response));
    }

    @GetMapping("/disputes")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<DisputeResponse>>> getDisputes(
            @RequestParam(required = false) DisputeStatus status,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<DisputeResponse> response = adminService.getDisputes(principal, status);
        return ResponseEntity.ok(ApiResponse.success("Customer disputes retrieved successfully", response));
    }

    @PostMapping("/disputes")
    public ResponseEntity<ApiResponse<DisputeResponse>> createDispute(
            @Valid @RequestBody CreateDisputeRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        DisputeResponse response = adminService.createDispute(principal, request);
        return ResponseEntity.ok(ApiResponse.success("Dispute logged successfully", response));
    }

    @PostMapping("/disputes/{id}/decision")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DisputeResponse>> processDisputeDecision(
            @PathVariable Long id,
            @Valid @RequestBody DisputeDecisionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        DisputeResponse response = adminService.processDisputeDecision(principal, id, request);
        return ResponseEntity.ok(ApiResponse.success("Dispute resolution processed", response));
    }

    @GetMapping("/surge-pricing")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<SurgePricingDto>>> getSurgePricingRules(
            @AuthenticationPrincipal UserPrincipal principal) {

        List<SurgePricingDto> response = adminService.getSurgePricingRules(principal);
        return ResponseEntity.ok(ApiResponse.success("Surge pricing rules retrieved", response));
    }

    @PutMapping("/surge-pricing")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SurgePricingDto>> updateSurgePricingRule(
            @Valid @RequestBody SurgePricingDto request,
            @AuthenticationPrincipal UserPrincipal principal) {

        SurgePricingDto response = adminService.updateSurgePricingRule(principal, request);
        return ResponseEntity.ok(ApiResponse.success("Surge pricing rule updated", response));
    }

    @PostMapping("/catalog/products/{id}/price-override")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> overrideProductPrice(
            @PathVariable Long id,
            @Valid @RequestBody PriceOverrideRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Product product = adminService.overrideProductPrice(principal, id, request);
        ProductResponse response = ProductMapper.toResponse(product);
        return ResponseEntity.ok(ApiResponse.success("Product price override applied", response));
    }

    @PostMapping("/catalog/bulk-import")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<BulkCatalogImportResult>> bulkImportCatalog(
            @RequestBody String csvContent,
            @AuthenticationPrincipal UserPrincipal principal) {

        BulkCatalogImportResult response = adminService.bulkImportCatalog(principal, csvContent);
        return ResponseEntity.ok(ApiResponse.success("Bulk catalog import processed", response));
    }
}
