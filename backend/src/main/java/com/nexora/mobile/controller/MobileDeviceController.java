package com.nexora.mobile.controller;

import com.nexora.auth.security.UserPrincipal;
import com.nexora.common.ApiResponse;
import com.nexora.mobile.dto.DeviceRegistrationRequest;
import com.nexora.mobile.dto.DeviceRegistrationResponse;
import com.nexora.mobile.service.MobileDeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mobile/device")
@RequiredArgsConstructor
public class MobileDeviceController {

    private final MobileDeviceService mobileDeviceService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DeviceRegistrationResponse>> registerDevice(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody DeviceRegistrationRequest request) {

        DeviceRegistrationResponse response = mobileDeviceService.registerDevice(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Mobile device registered successfully", response));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DeviceRegistrationResponse>>> getMyDevices(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        List<DeviceRegistrationResponse> devices = mobileDeviceService.getUserActiveDevices(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Active registered devices retrieved", devices));
    }

    @DeleteMapping("/{deviceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deactivateDevice(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable String deviceId) {

        mobileDeviceService.deactivateDevice(currentUser.getId(), deviceId);
        return ResponseEntity.ok(ApiResponse.success("Mobile device deactivated successfully", null));
    }
}
