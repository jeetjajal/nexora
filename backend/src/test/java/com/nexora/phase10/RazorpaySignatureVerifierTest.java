package com.nexora.phase10;

import com.nexora.payment.security.RazorpaySignatureVerifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests — no Spring context needed. We independently compute
 * the SAME HMAC-SHA256 algorithm RazorpaySignatureVerifier uses
 * internally (this is exactly what a real Razorpay server does when
 * it signs a payload) to produce genuinely valid signatures for the
 * "should accept" tests, and deliberately wrong ones for the
 * "should reject" tests.
 */
@DisplayName("RazorpaySignatureVerifier unit tests (Phase 10)")
class RazorpaySignatureVerifierTest {

    private static final String KEY_SECRET = "test-key-secret-value";
    private static final String WEBHOOK_SECRET = "test-webhook-secret-value";

    private final RazorpaySignatureVerifier verifier = new RazorpaySignatureVerifier(KEY_SECRET, WEBHOOK_SECRET);

    /** Mirrors the algorithm under test, to compute a genuinely correct signature for a given payload+secret. */
    private static String computeExpectedSignature(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // ==============================================================
    // Payment (client-callback) signature
    // ==============================================================

    @Test
    @DisplayName("A correctly computed payment signature is accepted")
    void validPaymentSignatureAccepted() throws Exception {
        String orderId = "order_ABC123";
        String paymentId = "pay_XYZ789";
        String correctSignature = computeExpectedSignature(orderId + "|" + paymentId, KEY_SECRET);

        assertThat(verifier.verifyPaymentSignature(orderId, paymentId, correctSignature)).isTrue();
    }

    @Test
    @DisplayName("A payment signature computed with the WRONG secret is rejected")
    void paymentSignatureWithWrongSecretRejected() throws Exception {
        String orderId = "order_ABC123";
        String paymentId = "pay_XYZ789";
        String signatureFromWrongSecret = computeExpectedSignature(orderId + "|" + paymentId, "a-completely-different-secret");

        assertThat(verifier.verifyPaymentSignature(orderId, paymentId, signatureFromWrongSecret)).isFalse();
    }

    @Test
    @DisplayName("A payment signature for a DIFFERENT order/payment id pair is rejected")
    void paymentSignatureForDifferentDataRejected() throws Exception {
        String signatureForDifferentPair = computeExpectedSignature("order_OTHER|pay_OTHER", KEY_SECRET);

        assertThat(verifier.verifyPaymentSignature("order_ABC123", "pay_XYZ789", signatureForDifferentPair)).isFalse();
    }

    @Test
    @DisplayName("A completely garbage payment signature is rejected")
    void garbagePaymentSignatureRejected() {
        assertThat(verifier.verifyPaymentSignature("order_ABC123", "pay_XYZ789", "not-a-real-signature")).isFalse();
    }

    @Test
    @DisplayName("A null or blank payment signature is rejected, not thrown")
    void nullOrBlankPaymentSignatureRejected() {
        assertThat(verifier.verifyPaymentSignature("order_ABC123", "pay_XYZ789", null)).isFalse();
        assertThat(verifier.verifyPaymentSignature("order_ABC123", "pay_XYZ789", "")).isFalse();
    }

    // ==============================================================
    // Webhook signature
    // ==============================================================

    @Test
    @DisplayName("A correctly computed webhook signature over the raw body is accepted")
    void validWebhookSignatureAccepted() throws Exception {
        String rawBody = "{\"event\":\"payment.captured\",\"payload\":{}}";
        String correctSignature = computeExpectedSignature(rawBody, WEBHOOK_SECRET);

        assertThat(verifier.verifyWebhookSignature(rawBody, correctSignature)).isTrue();
    }

    @Test
    @DisplayName("A webhook signature computed with the WRONG secret is rejected")
    void webhookSignatureWithWrongSecretRejected() throws Exception {
        String rawBody = "{\"event\":\"payment.captured\",\"payload\":{}}";
        String wrongSignature = computeExpectedSignature(rawBody, "a-completely-different-secret");

        assertThat(verifier.verifyWebhookSignature(rawBody, wrongSignature)).isFalse();
    }

    @Test
    @DisplayName("A webhook signature is rejected if even ONE character of the body changed after signing")
    void webhookSignatureRejectedIfBodyTampered() throws Exception {
        String originalBody = "{\"event\":\"payment.captured\",\"payload\":{\"amount\":100}}";
        String signatureForOriginal = computeExpectedSignature(originalBody, WEBHOOK_SECRET);

        // Attacker tampers with the amount after the signature was computed
        String tamperedBody = "{\"event\":\"payment.captured\",\"payload\":{\"amount\":999999}}";

        assertThat(verifier.verifyWebhookSignature(tamperedBody, signatureForOriginal)).isFalse();
    }

    @Test
    @DisplayName("Payment secret and webhook secret are genuinely independent — a valid payment signature is not a valid webhook signature for the same content")
    void paymentAndWebhookSecretsAreIndependent() throws Exception {
        String content = "order_ABC123|pay_XYZ789";
        String signatureUnderKeySecret = computeExpectedSignature(content, KEY_SECRET);

        // A signature valid for the payment-signature scheme should NOT
        // validate as a webhook signature over the same raw string,
        // because the two use different secrets.
        assertThat(verifier.verifyWebhookSignature(content, signatureUnderKeySecret)).isFalse();
    }
}
