package com.nexora.delivery.controller;

import com.nexora.auth.security.UserPrincipal;
import com.nexora.common.ApiResponse;
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

@RestController
@RequestMapping("/api/v1/delivery")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping("/profile")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<ApiResponse<DeliveryPartnerProfileResponse>> getProfile(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        DeliveryPartnerProfileResponse profile = deliveryService.getProfile(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", profile));
    }

    @PostMapping("/status")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<ApiResponse<DeliveryPartnerProfileResponse>> toggleOnlineStatus(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam boolean isOnline) {
            
        DeliveryPartnerProfileResponse profile = deliveryService.toggleOnlineStatus(currentUser.getId(), isOnline);
        return ResponseEntity.ok(ApiResponse.success("Status updated successfully", profile));
    }

    @PostMapping("/location")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<ApiResponse<Void>> updateLocation(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody LocationUpdateRequest request) {
            
        deliveryService.updateLocation(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Location updated successfully", null));
    }

    @PostMapping("/assignments/{id}/accept")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<ApiResponse<DeliveryAssignmentResponse>> acceptAssignment(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id) {
            
        DeliveryAssignmentResponse response = deliveryService.acceptAssignment(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Assignment accepted successfully", response));
    }

    @PutMapping("/assignments/{id}/status")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<ApiResponse<DeliveryAssignmentResponse>> updateAssignmentStatus(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id,
            @RequestParam AssignmentStatus status) {
            
        DeliveryAssignmentResponse response = deliveryService.updateAssignmentStatus(currentUser.getId(), id, status);
        return ResponseEntity.ok(ApiResponse.success("Assignment status updated successfully", response));
    }
}
