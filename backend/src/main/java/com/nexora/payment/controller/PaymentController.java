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

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

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

    @PostMapping("/api/v1/payments/{paymentId}/result")
    public ResponseEntity<ApiResponse<PaymentResponse>> recordClientReportedResult(
            @PathVariable Long paymentId,
            @Valid @RequestBody PaymentResultRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        PaymentResponse response = paymentService.recordClientReportedResult(principal, paymentId, request);
        return ResponseEntity.ok(ApiResponse.success(
                "Result recorded — final confirmation still pending verification", response));
    }

    @PostMapping("/api/v1/payments/{paymentId}/refund")
    @PreAuthorize("hasAnyRole('STORE_OWNER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(
            @PathVariable Long paymentId,
            @AuthenticationPrincipal UserPrincipal principal) {

        PaymentResponse response = paymentService.refundPayment(principal, paymentId);
        return ResponseEntity.ok(ApiResponse.success("Payment refunded successfully", response));
    }

    @PostMapping("/api/v1/payments/{paymentId}/refund-partial")
    @PreAuthorize("hasAnyRole('STORE_OWNER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPaymentPartial(
            @PathVariable Long paymentId,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal UserPrincipal principal) {

        PaymentResponse response = paymentService.refundPaymentPartial(principal, paymentId, amount, reason);
        return ResponseEntity.ok(ApiResponse.success("Partial payment refund processed successfully", response));
    }

    @GetMapping("/api/v1/payments/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(
            @PathVariable Long paymentId,
            @AuthenticationPrincipal UserPrincipal principal) {

        PaymentResponse response = paymentService.getPaymentById(principal, paymentId);
        return ResponseEntity.ok(ApiResponse.success("Payment fetched successfully", response));
    }

    @GetMapping("/api/v1/orders/{orderId}/payments")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentsForOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<PaymentResponse> response = paymentService.getPaymentsForOrder(principal, orderId);
        return ResponseEntity.ok(ApiResponse.success("Payments fetched successfully", response));
    }
}
