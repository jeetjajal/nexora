package com.nexora.payment.gateway;

import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * THE ONLY CLASS IN THIS ENTIRE CODEBASE THAT IMPORTS com.razorpay.*.
 * Everything else depends on the PaymentGatewayClient interface — see
 * its javadoc for why that boundary matters.
 *
 * CREDENTIALS: read from environment variables, never hard-coded —
 * same pattern as the JWT secret (Phase 3). Get real TEST MODE
 * credentials from your own Razorpay dashboard (Settings -> API Keys)
 * — they'll look like "rzp_test_xxxxxxxxxx" for the key id. NEVER use
 * live-mode credentials during development.
 *
 * AMOUNT UNITS: Razorpay's API expects amounts in the currency's
 * smallest unit — paise for INR, i.e. amount * 100 — as an integer,
 * not rupees as a decimal. Getting this conversion wrong is a classic
 * integration bug (charging 1/100th or 100x the intended amount), so
 * it's isolated to one line per method rather than something every
 * caller has to remember.
 *
 * SDK VERIFICATION NOTE (refundPayment, Phase 11): the installed
 * Razorpay Java SDK 1.4.5 exposes the resource clients as lowercase
 * fields on RazorpayClient:
 *
 *     razorpayClient.orders
 *     razorpayClient.payments
 *
 * Therefore createOrder uses orders.create(...) and refundPayment
 * uses payments.refund(...).
 *
 * LAZY CLIENT CONSTRUCTION: the real RazorpayClient is built on FIRST
 * USE (inside createOrder/refundPayment), not in this bean's constructor.
 * If it were built eagerly at startup, a missing/placeholder credential
 * would fail the ENTIRE application's startup — meaning nobody could
 * even try Phases 1-8's endpoints without first setting up a Razorpay
 * account. Building it lazily means the app always starts cleanly;
 * only an actual attempt to initiate a payment fails (with a clean
 * PaymentGatewayException) until real credentials are configured.
 *
 * @Profile("!test"): this bean is EXCLUDED whenever the "test" Spring
 * profile is active (which src/test/resources/application.properties
 * turns on for the whole test suite) — StubPaymentGatewayClient
 * provides the substitute there instead.
 */
@Component
@Profile("!test")
public class RazorpayGatewayClientImpl implements PaymentGatewayClient {

    private final String keyId;
    private final String keySecret;

    private volatile RazorpayClient razorpayClient;

    public RazorpayGatewayClientImpl(
            @Value("${razorpay.key-id}") String keyId,
            @Value("${razorpay.key-secret}") String keySecret) {

        this.keyId = keyId;
        this.keySecret = keySecret;
    }

    @Override
    public RazorpayOrderResult createOrder(
            BigDecimal amount,
            String currency,
            String receipt) {

        try {
            JSONObject orderRequest = new JSONObject();

            // Razorpay expects the amount in the smallest currency unit.
            // For INR: rupees -> paise.
            orderRequest.put(
                    "amount",
                    amount.multiply(BigDecimal.valueOf(100)).intValue()
            );

            orderRequest.put("currency", currency);
            orderRequest.put("receipt", receipt);

            /*
             * Razorpay Java SDK 1.4.5 uses the lowercase `orders` resource.
             */
            com.razorpay.Order razorpayOrder =
                    getOrCreateClient().orders.create(orderRequest);

            return new RazorpayOrderResult(
                    razorpayOrder.get("id"),
                    razorpayOrder.get("status")
            );

        } catch (Exception ex) {
            throw new PaymentGatewayException(
                    "Razorpay order creation failed: " + ex.getMessage(),
                    ex
            );
        }
    }

    @Override
    public RazorpayRefundResult refundPayment(
            String razorpayPaymentId,
            BigDecimal amount) {

        try {
            JSONObject refundRequest = new JSONObject();

            // Razorpay expects the refund amount in paise for INR.
            refundRequest.put(
                    "amount",
                    amount.multiply(BigDecimal.valueOf(100)).intValue()
            );

            /*
             * Razorpay Java SDK 1.4.5 uses the lowercase `payments` resource.
             */
            com.razorpay.Refund razorpayRefund =
                    getOrCreateClient().payments.refund(
                            razorpayPaymentId,
                            refundRequest
                    );

            return new RazorpayRefundResult(
                    razorpayRefund.get("id"),
                    razorpayRefund.get("status")
            );

        } catch (Exception ex) {
            throw new PaymentGatewayException(
                    "Razorpay refund failed: " + ex.getMessage(),
                    ex
            );
        }
    }

    /**
     * Double-checked lazy initialization.
     *
     * The RazorpayClient is:
     * 1. Created only when the first real gateway call happens.
     * 2. Created at most once.
     * 3. Reused for subsequent requests.
     */
    private RazorpayClient getOrCreateClient() throws Exception {

        RazorpayClient client = razorpayClient;

        if (client == null) {

            synchronized (this) {

                client = razorpayClient;

                if (client == null) {

                    client = new RazorpayClient(
                            keyId,
                            keySecret
                    );

                    razorpayClient = client;
                }
            }
        }

        return client;
    }
}