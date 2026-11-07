package com.nexora.wms.controller;

import com.nexora.common.ApiResponse;
import com.nexora.wms.dto.BarcodeResolveRequest;
import com.nexora.wms.dto.BarcodeResolveResponse;
import com.nexora.wms.service.BarcodeScanningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * WMS BARCODE & QR RESOLUTION CONTROLLER (NEXORA 3.0 WS2)
 */
@RestController
@RequestMapping("/api/v1/wms/barcode")
@RequiredArgsConstructor
public class WmsBarcodeController {

    private final BarcodeScanningService barcodeScanningService;

    @PostMapping("/resolve")
    @PreAuthorize("hasAnyRole('STORE_MANAGER','PICKER','PACKER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<BarcodeResolveResponse>> resolveBarcode(
            @Valid @RequestBody BarcodeResolveRequest request) {
        BarcodeResolveResponse response = barcodeScanningService.resolveBarcodeDetails(
                request.getStoreId(), request.getBarcode());
        return ResponseEntity.ok(ApiResponse.success("Barcode resolved successfully", response));
    }
}
