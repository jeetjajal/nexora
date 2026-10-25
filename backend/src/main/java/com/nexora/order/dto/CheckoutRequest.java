package com.nexora.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Deliberately minimal: checkout needs nothing except WHERE to
 * deliver. Everything else — which products, quantities, prices — is
 * derived entirely from the caller's own Cart (Phase 7) and current
 * Product data on the backend. There is no field here for price,
 * items, or totals; the client cannot influence any of those.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {

    @NotNull(message = "deliveryAddressId is required")
    private Long deliveryAddressId;
}
