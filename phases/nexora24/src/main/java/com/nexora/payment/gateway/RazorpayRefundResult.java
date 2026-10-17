package com.nexora.payment.gateway;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * What we need back from the gateway after asking it to refund a
 * payment — mirrors RazorpayOrderResult's reasoning: a small,
 * gateway-agnostic result type, nothing SDK-specific leaks past this
 * boundary.
 */
@Getter
@AllArgsConstructor
public class RazorpayRefundResult {
    private final String razorpayRefundId;
    private final String status; // Razorpay's own refund status string, e.g. "processed"
}
