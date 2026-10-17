package com.nexora.payment.controller;

import com.nexora.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * PUBLIC ON PURPOSE — see SecurityConfig, where
 * "/api/v1/payments/webhook" is added to the permitAll() list.
 * Razorpay's servers call this directly; they have no Nexora JWT and
 * never will. The entire security model for this endpoint is
 * PaymentService.handleWebhookEvent()'s signature verification, not
 * Spring Security's usual authentication — this is the standard,
 * correct way webhooks are secured industry-wide: a shared-secret
 * HMAC signature proves the caller holds a secret only Razorpay and
 * our backend know, which is a stronger, more specific guarantee than
 * "presented some bearer token" would be anyway.
 *
 * @RequestBody String (not a DTO): binds the raw, untouched request
 * body — required because the signature is computed over those exact
 * bytes. Deserializing into a DTO first and re-serializing to check
 * the signature could silently change whitespace/key ordering and
 * break verification. See PaymentService.handleWebhookEvent's javadoc.
 */
@RestController
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentService paymentService;

    @PostMapping("/api/v1/payments/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String rawRequestBody,
            @RequestHeader("X-Razorpay-Signature") String signature) {

        paymentService.handleWebhookEvent(rawRequestBody, signature);

        // Razorpay only cares about the HTTP status code (2xx = stop
        // retrying); an empty 200 body is standard practice for
        // webhook acknowledgments.
        return ResponseEntity.ok().build();
    }
}
