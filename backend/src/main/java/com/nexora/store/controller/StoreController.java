package com.nexora.store.controller;

import com.nexora.auth.security.UserPrincipal;
import com.nexora.common.ApiResponse;
import com.nexora.store.dto.StoreRequest;
import com.nexora.store.dto.StoreResponse;
import com.nexora.store.dto.StoreStatusRequest;
import com.nexora.store.entity.StoreStatus;
import com.nexora.store.service.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * All endpoints here require a valid JWT (enforced globally by
 * SecurityConfig's "anyRequest().authenticated()" rule from Phase 3 —
 * unchanged). @PreAuthorize below adds a SECOND, more specific layer:
 * which roles may call which endpoint. Resource-level ownership
 * (e.g. "is this really YOUR store?") is enforced inside StoreService,
 * not here — the controller only knows about roles, not data.
 */
@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    /**
     * Only STORE_OWNER or ADMIN can create a store. A plain CUSTOMER
     * or DELIVERY_PARTNER token is rejected with 403 before this
     * method body even runs.
     *
     * Sample request:
     * {
     *   "name": "Rajkot Pizza Hub",
     *   "description": "Wood-fired pizza, fast delivery.",
     *   "imageUrl": "https://example.com/store.jpg",
     *   "openingTime": "10:00:00",
     *   "closingTime": "23:00:00",
     *   "categoryNames": ["Pizza", "Fast Food"]
     * }
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('STORE_OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<StoreResponse>> createStore(
            @Valid @RequestBody StoreRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        StoreResponse response = storeService.createStore(request, principal);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Store created successfully", response));
    }

    /**
     * PHASE 4: any authenticated user (any role) can browse stores,
     * paginated. PHASE 5: now also supports optional search/filter,
     * e.g. GET /api/v1/stores?name=pizza&categoryName=Fast%20Food&status=OPEN&sort=name,asc
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<StoreResponse>>> getAllStores(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) StoreStatus status,
            @PageableDefault(size = 10) Pageable pageable) {

        Page<StoreResponse> response = storeService.searchStores(name, categoryName, status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Stores fetched successfully", response));
    }

    /**
     * Any authenticated user can view a single store's details.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StoreResponse>> getStoreById(@PathVariable Long id) {
        StoreResponse response = storeService.getStoreById(id);
        return ResponseEntity.ok(ApiResponse.success("Store fetched successfully", response));
    }

    /**
     * A STORE_OWNER's own dashboard listing: "show me all stores I own."
     * ADMIN can call this too (they just won't own any stores unless
     * they also happen to be a store owner) — kept role-gated to
     * STORE_OWNER/ADMIN since a CUSTOMER has no meaningful use for it.
     */
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('STORE_OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<List<StoreResponse>>> getMyStores(
            @AuthenticationPrincipal UserPrincipal principal) {

        List<StoreResponse> response = storeService.getStoresOwnedByCurrentUser(principal);
        return ResponseEntity.ok(ApiResponse.success("Your stores fetched successfully", response));
    }

    /**
     * Update a store's editable fields. Role check here is just
     * "STORE_OWNER or ADMIN" — StoreService then verifies the caller
     * actually owns THIS store (unless they're an ADMIN).
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('STORE_OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<StoreResponse>> updateStore(
            @PathVariable Long id,
            @Valid @RequestBody StoreRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        StoreResponse response = storeService.updateStore(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success("Store updated successfully", response));
    }

    /**
     * Change just the store's operating status (OPEN / CLOSED / SUSPENDED).
     * Kept as its own endpoint (PATCH, not PUT) since it's a much
     * smaller, more frequent operation than a full store edit.
     *
     * Sample request: { "status": "CLOSED" }
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('STORE_OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<StoreResponse>> updateStoreStatus(
            @PathVariable Long id,
            @Valid @RequestBody StoreStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        StoreResponse response = storeService.updateStoreStatus(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success("Store status updated successfully", response));
    }
}
