package com.nexora.order.controller;

import com.nexora.auth.security.UserPrincipal;
import com.nexora.common.ApiResponse;
import com.nexora.order.dto.DirectCheckoutRequest;
import com.nexora.order.dto.VerifyDeliveryOtpRequest;
import com.nexora.order.dto.OrderTrackingResponse;
import com.nexora.order.dto.CheckoutRequest;
import com.nexora.order.dto.OrderResponse;
import com.nexora.order.dto.OrderStatusUpdateRequest;
import com.nexora.order.service.OrderService;
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

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Turns the caller's current cart into one or more orders (one per
     * distinct store represented in the cart — see OrderService.checkout).
     * No role restriction, same reasoning as Cart (Phase 7): any
     * authenticated user may check out their own cart.
     *
     * PHASE 11: an optional `Idempotency-Key` header protects against
     * the roadmap's exact scenario — network fails after this request
     * reaches the server, the client retries. Send any unique string
     * (a UUID is the usual choice); the SAME key on a retried request
     * returns the original result without creating duplicate orders.
     * See IdempotencyKey's javadoc for the full mechanism. The header
     * is optional — omitting it behaves exactly as it did before this
     * phase (no deduplication tracking at all).
     *
     * Sample request: { "deliveryAddressId": 12 }
     */
    @PostMapping("/api/v1/orders/checkout")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> checkout(
            @Valid @RequestBody CheckoutRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<OrderResponse> response = orderService.checkout(principal, request, idempotencyKey);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order(s) placed successfully", response));
    }

    /** The caller's own order history, paginated. */
    @GetMapping("/api/v1/orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10) Pageable pageable) {

        Page<OrderResponse> response = orderService.getMyOrders(principal, pageable);
        return ResponseEntity.ok(ApiResponse.success("Orders fetched successfully", response));
    }

    /**
     * A single order's details. Authorized inside OrderService: the
     * customer who placed it, the owner of the order's store, or ADMIN.
     */
    @GetMapping("/api/v1/orders/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        OrderResponse response = orderService.getOrderById(principal, id);
        return ResponseEntity.ok(ApiResponse.success("Order fetched successfully", response));
    }

    /**
     * Customer self-service cancellation — only while the order hasn't
     * progressed past PAYMENT_PENDING. See OrderService.cancelOwnOrder.
     */
    @PostMapping("/api/v1/orders/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOwnOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        OrderResponse response = orderService.cancelOwnOrder(principal, id);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", response));
    }

    /**
     * The full store-side state machine (CREATED through DELIVERED,
     * plus REJECTED/CANCELLED). STORE_OWNER (of that order's store) or
     * ADMIN only. Invalid transitions are rejected with 409 — see
     * OrderService.ALLOWED_TRANSITIONS.
     *
     * Sample request: { "status": "CONFIRMED" }
     */
    @PatchMapping("/api/v1/orders/{id}/status")
    @PreAuthorize("hasAnyRole('STORE_OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        OrderResponse response = orderService.updateStatus(principal, id, request);
        return ResponseEntity.ok(ApiResponse.success("Order status updated successfully", response));
    }

    /**
     * A store owner's (or admin's) list of orders placed against one
     * specific store, paginated.
     */
    @GetMapping("/api/v1/stores/{storeId}/orders")
    @PreAuthorize("hasAnyRole('STORE_OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getStoreOrders(
            @PathVariable Long storeId,
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10) Pageable pageable) {

        Page<OrderResponse> response = orderService.getStoreOrders(principal, storeId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Store orders fetched successfully", response));
    }

    /**
     * PHASE 6: Direct transactional checkout with authoritative server pricing
     * and atomic stock reduction.
     */
    @PostMapping("/api/v1/orders/direct")
    public ResponseEntity<ApiResponse<OrderResponse>> directCheckout(
            @Valid @RequestBody DirectCheckoutRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal) {

        OrderResponse response = orderService.directCheckout(principal, request, idempotencyKey);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully and routed to dark store", response));
    }

    /**
     * PHASE 6: Real-time customer order tracking with status timeline.
     */
    @GetMapping("/api/v1/orders/{id}/tracking")
    public ResponseEntity<ApiResponse<OrderTrackingResponse>> getOrderTracking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        OrderTrackingResponse response = orderService.getOrderTracking(principal, id);
        return ResponseEntity.ok(ApiResponse.success("Order tracking fetched successfully", response));
    }

    /**
     * PHASE 6: Delivery Partner OTP verification at doorstep.
     */
    @PostMapping("/api/v1/orders/{id}/verify-delivery")
    @PreAuthorize("hasAnyRole('DELIVERY_PARTNER','STORE_OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> verifyDeliveryOtp(
            @PathVariable Long id,
            @Valid @RequestBody VerifyDeliveryOtpRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        OrderResponse response = orderService.verifyDeliveryOtp(principal, id, request);
        return ResponseEntity.ok(ApiResponse.success("Delivery OTP verified. Order marked as DELIVERED.", response));
    }

}
