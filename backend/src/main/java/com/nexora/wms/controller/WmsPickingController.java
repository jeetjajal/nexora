package com.nexora.wms.controller;

import com.nexora.auth.security.UserPrincipal;
import com.nexora.common.ApiResponse;
import com.nexora.wms.dto.PickTaskResponse;
import com.nexora.wms.dto.ReportExceptionRequest;
import com.nexora.wms.dto.ScanItemPickRequest;
import com.nexora.wms.dto.ScanItemPickResponse;
import com.nexora.wms.entity.PickTask;
import com.nexora.wms.entity.PickTaskItem;
import com.nexora.wms.service.PickerAssignmentService;
import com.nexora.wms.service.WmsFulfillmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * WMS PICKING & S-SHAPE ROUTE CONTROLLER (NEXORA 3.0 WS3, WS4)
 */
@RestController
@RequestMapping("/api/v1/wms")
@RequiredArgsConstructor
public class WmsPickingController {

    private final WmsFulfillmentService wmsFulfillmentService;
    private final PickerAssignmentService pickerAssignmentService;

    @GetMapping("/orders/{orderId}/picklist")
    @PreAuthorize("hasAnyRole('STORE_MANAGER','PICKER','PACKER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PickTaskResponse>> getPicklist(@PathVariable Long orderId) {
        PickTaskResponse response = wmsFulfillmentService.getPicklistForOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success("Picklist retrieved successfully", response));
    }

    @PostMapping("/picklist/items/{itemId}/scan")
    @PreAuthorize("hasAnyRole('STORE_MANAGER','PICKER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ScanItemPickResponse>> scanItemPick(
            @PathVariable Long itemId,
            @Valid @RequestBody ScanItemPickRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Long userId = currentUser != null ? currentUser.getId() : 1L;
        ScanItemPickResponse response = wmsFulfillmentService.scanItemPick(itemId, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Item picked successfully", response));
    }

    @PostMapping("/picklist/items/{itemId}/exception")
    @PreAuthorize("hasAnyRole('STORE_MANAGER','PICKER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PickTaskItem>> reportPickException(
            @PathVariable Long itemId,
            @Valid @RequestBody ReportExceptionRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Long userId = currentUser != null ? currentUser.getId() : 1L;
        PickTaskItem item = wmsFulfillmentService.reportPickException(itemId, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Pick exception reported successfully", item));
    }

    @PostMapping("/tasks/{taskId}/reassign")
    @PreAuthorize("hasAnyRole('STORE_MANAGER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PickTask>> reassignPicker(
            @PathVariable Long taskId,
            @RequestParam Long newPickerProfileId,
            @RequestParam(defaultValue = "Manual manager reassignment") String reason,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Long userId = currentUser != null ? currentUser.getId() : 1L;
        PickTask task = pickerAssignmentService.reassignPicker(taskId, newPickerProfileId, reason, userId);
        return ResponseEntity.ok(ApiResponse.success("Task reassigned successfully", task));
    }
}
