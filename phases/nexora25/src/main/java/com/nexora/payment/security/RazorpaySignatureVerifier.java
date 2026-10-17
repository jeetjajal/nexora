package com.nexora.payment.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * THE CRYPTOGRAPHIC CORE OF PHASE 10.
 *
 * Razorpay offers TWO different signatures for TWO different purposes
 * — it's important not to conflate them:
 *
 * 1. PAYMENT (CLIENT-CALLBACK) SIGNATURE — verifyPaymentSignature().
 *    After Razorpay's Checkout widget completes in the customer's
 *    browser, it hands the frontend razorpay_order_id,
 *    razorpay_payment_id, and razorpay_signature. That signature is
 *    HMAC-SHA256("{order_id}|{payment_id}", key_secret) — signed with
 *    the SAME secret used to create the order (razorpay.key-secret).
 *    Verifying it proves the (order_id, payment_id) pair genuinely
 *    came from Razorpay, since only Razorpay and our own backend ever
 *    know key_secret — a client can't forge this without it.
 *
 * 2. WEBHOOK SIGNATURE — verifyWebhookSignature().
 *    Razorpay's servers call OUR server directly (no browser
 *    involved) with the full event payload and an
 *    X-Razorpay-Signature header. That signature is
 *    HMAC-SHA256(raw_request_body, webhook_secret) — a DIFFERENT
 *    secret (razorpay.webhook-secret), configured separately in the
 *    Razorpay dashboard specifically for webhooks. This is the more
 *    resilient confirmation path: it doesn't depend on the customer's
 *    browser successfully calling back at all.
 *
 * BOTH are legitimate, independent proofs that data genuinely
 * originated from Razorpay — PaymentService accepts either as
 * sufficient to mark a payment SUCCESS (see its javadoc).
 *
 * CONSTANT-TIME COMPARISON: we use MessageDigest.isEqual(), not
 * String.equals() or Arrays.equals(), to compare the computed and
 * provided signatures. A naive equals() comparison returns as soon as
 * it finds the first mismatched byte, meaning a wrong signature that
 * happens to share more of its prefix with the correct one takes
 * measurably longer to reject — an attacker who can measure that
 * timing difference could, byte by byte, forge a valid signature
 * without ever knowing the secret. MessageDigest.isEqual() always
 * compares the full length, closing that side-channel.
 */
@Component
public class RazorpaySignatureVerifier {

    private final String keySecret;
    private final String webhookSecret;

    public RazorpaySignatureVerifier(
            @Value("${razorpay.key-secret}") String keySecret,
            @Value("${razorpay.webhook-secret}") String webhookSecret) {
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
    }

    public boolean verifyPaymentSignature(String razorpayOrderId, String razorpayPaymentId, String providedSignature) {
        String payload = razorpayOrderId + "|" + razorpayPaymentId;
        return verify(payload, providedSignature, keySecret);
    }

    public boolean verifyWebhookSignature(String rawRequestBody, String providedSignature) {
        return verify(rawRequestBody, providedSignature, webhookSecret);
    }

    private boolean verify(String payload, String providedSignature, String secret) {
        if (providedSignature == null || providedSignature.isBlank()) {
            return false;
        }
        String computedSignature = computeHmacSha256Hex(payload, secret);
        return MessageDigest.isEqual(
                computedSignature.getBytes(StandardCharsets.UTF_8),
                providedSignature.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String computeHmacSha256Hex(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return toHex(hmacBytes);
        } catch (Exception ex) {
            // NoSuchAlgorithmException / InvalidKeyException — HmacSHA256
            // is a standard JDK algorithm always available, so this path
            // should be unreachable in practice; wrapping defensively
            // rather than declaring checked exceptions everywhere.
            throw new IllegalStateException("Failed to compute HMAC-SHA256 signature", ex);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
