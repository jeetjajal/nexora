package com.nexora.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private Long id;
    private Long orderId;
    private String razorpayOrderId;

    /**
     * The PUBLIC key id — safe to send to the frontend. This is how
     * Razorpay's Checkout.js widget is meant to be initialized
     * client-side; it is NOT a secret (only razorpay.key-secret,
     * which never leaves the backend, is sensitive).
     */
    private String razorpayKeyId;

    private BigDecimal amount;
    private String currency;
    private String status;
    private LocalDateTime createdAt;
}
