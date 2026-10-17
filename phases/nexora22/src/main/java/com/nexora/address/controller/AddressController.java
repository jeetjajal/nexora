package com.nexora.address.controller;

import com.nexora.address.dto.AddressRequest;
import com.nexora.address.dto.AddressResponse;
import com.nexora.address.service.AddressService;
import com.nexora.auth.security.UserPrincipal;
import com.nexora.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Same "always the caller's own resource" pattern CartController
 * established in Phase 7 — there's no {userId} anywhere in these
 * paths; every operation resolves to whoever the JWT identifies.
 */
@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(
            @Valid @RequestBody AddressRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        AddressResponse response = addressService.createAddress(principal, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Address created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getMyAddresses(
            @AuthenticationPrincipal UserPrincipal principal) {

        List<AddressResponse> response = addressService.getMyAddresses(principal);
        return ResponseEntity.ok(ApiResponse.success("Addresses fetched successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressResponse>> getAddressById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        AddressResponse response = addressService.getAddressById(principal, id);
        return ResponseEntity.ok(ApiResponse.success("Address fetched successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        AddressResponse response = addressService.updateAddress(principal, id, request);
        return ResponseEntity.ok(ApiResponse.success("Address updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        addressService.deleteAddress(principal, id);
        return ResponseEntity.ok(ApiResponse.success("Address deleted successfully", null));
    }
}
