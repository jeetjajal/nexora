package com.nexora.payment.controller;

import com.nexora.auth.security.UserPrincipal;
import com.nexora.common.ApiResponse;
import com.nexora.payment.dto.PaymentResponse;
import com.nexora.payment.dto.PaymentResultRequest;
import com.nexora.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Initiates payment for an order the caller owns. No role
     * restriction — same reasoning as checkout (Phase 8): any
     * authenticated user pays for their own order.
     */
    @PostMapping("/api/v1/orders/{orderId}/payments")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiatePayment(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal) {

        PaymentResponse response = paymentService.initiatePayment(principal, orderId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment initiated successfully", response));
    }

    /**
     * Records what the frontend observed after the Razorpay Checkout
     * widget closed. See PaymentService's javadoc: this NEVER marks a
     * payment successful by itself — that requires Phase 10's verified
     * webhook.
     *
     * Sample request:
     * { "razorpayPaymentId": "pay_ABC123", "razorpaySignature": "..." }
     */
    @PostMapping("/api/v1/payments/{paymentId}/result")
    public ResponseEntity<ApiResponse<PaymentResponse>> recordClientReportedResult(
            @PathVariable Long paymentId,
            @Valid @RequestBody PaymentResultRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        PaymentResponse response = paymentService.recordClientReportedResult(principal, paymentId, request);
        return ResponseEntity.ok(ApiResponse.success(
                "Result recorded — final confirmation still pending verification", response));
    }

    @GetMapping("/api/v1/payments/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(
            @PathVariable Long paymentId,
            @AuthenticationPrincipal UserPrincipal principal) {

        PaymentResponse response = paymentService.getPaymentById(principal, paymentId);
        return ResponseEntity.ok(ApiResponse.success("Payment fetched successfully", response));
    }

    /** Every payment attempt for one order (there can be more than one if earlier attempts failed). */
    @GetMapping("/api/v1/orders/{orderId}/payments")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentsForOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<PaymentResponse> response = paymentService.getPaymentsForOrder(principal, orderId);
        return ResponseEntity.ok(ApiResponse.success("Payments fetched successfully", response));
    }
}
