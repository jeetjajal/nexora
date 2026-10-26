package com.nexora.delivery.controller;

import com.nexora.auth.security.UserPrincipal;
import com.nexora.common.ApiResponse;
import com.nexora.delivery.dto.DeliveryAssignmentDetailResponse;
import com.nexora.delivery.dto.DeliveryAssignmentResponse;
import com.nexora.delivery.dto.DeliveryPartnerProfileResponse;
import com.nexora.delivery.dto.LocationUpdateRequest;
import com.nexora.delivery.entity.AssignmentStatus;
import com.nexora.delivery.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/delivery")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('DELIVERY_PARTNER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DeliveryPartnerProfileResponse>> getProfile(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        DeliveryPartnerProfileResponse profile = deliveryService.getProfile(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", profile));
    }

    @PostMapping("/status")
    @PreAuthorize("hasAnyRole('DELIVERY_PARTNER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DeliveryPartnerProfileResponse>> toggleOnlineStatus(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam boolean isOnline) {
            
        DeliveryPartnerProfileResponse profile = deliveryService.toggleOnlineStatus(currentUser.getId(), isOnline);
        return ResponseEntity.ok(ApiResponse.success("Status updated successfully", profile));
    }

    @PostMapping("/location")
    @PreAuthorize("hasAnyRole('DELIVERY_PARTNER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateLocation(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody LocationUpdateRequest request) {
            
        deliveryService.updateLocation(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Location updated successfully", null));
    }

    @GetMapping("/assignments/my")
    @PreAuthorize("hasAnyRole('DELIVERY_PARTNER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<DeliveryAssignmentDetailResponse>>> getMyAssignments(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        List<DeliveryAssignmentDetailResponse> response = deliveryService.getMyAssignments(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Assignments retrieved successfully", response));
    }

    @PostMapping("/assignments/{id}/accept")
    @PreAuthorize("hasAnyRole('DELIVERY_PARTNER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DeliveryAssignmentResponse>> acceptAssignment(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id) {
            
        DeliveryAssignmentResponse response = deliveryService.acceptAssignment(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Assignment accepted successfully", response));
    }

    @PostMapping("/assignments/{id}/pickup")
    @PreAuthorize("hasAnyRole('DELIVERY_PARTNER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DeliveryAssignmentDetailResponse>> pickupAssignment(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id) {

        DeliveryAssignmentDetailResponse response = deliveryService.pickupAssignment(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Order picked up from dark store. Status is OUT_FOR_DELIVERY", response));
    }

    @PutMapping("/assignments/{id}/status")
    @PreAuthorize("hasAnyRole('DELIVERY_PARTNER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DeliveryAssignmentResponse>> updateAssignmentStatus(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id,
            @RequestParam AssignmentStatus status) {
            
        DeliveryAssignmentResponse response = deliveryService.updateAssignmentStatus(currentUser.getId(), id, status);
        return ResponseEntity.ok(ApiResponse.success("Assignment status updated successfully", response));
    }
}
