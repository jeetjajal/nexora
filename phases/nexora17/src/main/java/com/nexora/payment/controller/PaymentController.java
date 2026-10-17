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
import org.springframework.security.access.prepost.PreAuthorize;
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
     *
     * PHASE 11: an optional `Idempotency-Key` header protects against
     * a retried request creating a duplicate Razorpay order — see
     * OrderController.checkout's javadoc and IdempotencyKey's javadoc
     * for the full mechanism. Also as of Phase 11, a fresh attempt is
     * now genuinely possible after an earlier attempt failed (see
     * PaymentService.initiatePayment's javadoc on the retry-eligibility fix).
     */
    @PostMapping("/api/v1/orders/{orderId}/payments")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiatePayment(
            @PathVariable Long orderId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal) {

        PaymentResponse response = paymentService.initiatePayment(principal, orderId, idempotencyKey);
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

    /**
     * PHASE 11: refunds a SUCCESS payment in full. Restricted to the
     * order's STORE_OWNER or ADMIN — see PaymentService.refundPayment's
     * javadoc for why customers can't self-service this.
     */
    @PostMapping("/api/v1/payments/{paymentId}/refund")
    @PreAuthorize("hasAnyRole('STORE_OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(
            @PathVariable Long paymentId,
            @AuthenticationPrincipal UserPrincipal principal) {

        PaymentResponse response = paymentService.refundPayment(principal, paymentId);
        return ResponseEntity.ok(ApiResponse.success("Payment refunded successfully", response));
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
