package com.nexora.payment.gateway;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * What we need back from the payment gateway after asking it to
 * create an order — deliberately just these three fields, nothing
 * gateway-SDK-specific leaks past this boundary into the rest of the
 * application (see PaymentGatewayClient's javadoc for why that matters).
 */
@Getter
@AllArgsConstructor
public class RazorpayOrderResult {
    private final String razorpayOrderId;
    private final String status; // Razorpay's own order status string, e.g. "created"
}
