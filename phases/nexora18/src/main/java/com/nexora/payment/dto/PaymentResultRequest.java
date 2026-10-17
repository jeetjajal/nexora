package com.nexora.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * What the frontend sends after Razorpay's Checkout widget closes,
 * reporting what IT observed happened. See PaymentService's javadoc
 * for why this is recorded purely for audit and can NEVER, on its
 * own, mark a payment successful.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResultRequest {

    @NotBlank(message = "razorpayPaymentId is required")
    private String razorpayPaymentId;

    @NotBlank(message = "razorpaySignature is required")
    private String razorpaySignature;
}
