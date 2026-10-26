package com.nexora.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DirectCheckoutRequest {

    @NotNull(message = "deliveryAddressId is required")
    private Long deliveryAddressId;

    @NotEmpty(message = "items cannot be empty")
    @Valid
    private List<DirectCheckoutItemRequest> items;

    @Builder.Default
    private String paymentMethod = "RAZORPAY";

    private String couponCode;
}
