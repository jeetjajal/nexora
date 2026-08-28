package com.nexora.payment.gateway;

import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * THE ONLY CLASS IN THIS ENTIRE CODEBASE THAT IMPORTS com.razorpay.*.
 *
 * Everything else depends on the PaymentGatewayClient interface.
 *
 * CREDENTIALS:
 * Read from configuration/environment variables and never hard-coded.
 * Real development credentials must be Razorpay TEST MODE credentials.
 * Never use live-mode credentials during development.
 *
 * AMOUNT UNITS:
 * Razorpay expects amounts in the currency's smallest unit.
 * For INR:
 *
 *     1 rupee = 100 paise
 *
 * Therefore the amount is converted from rupees to paise before
 * sending the request to Razorpay.
 *
 * LAZY CLIENT CONSTRUCTION:
 * RazorpayClient is constructed only when createOrder() is actually
 * called. This allows the application to start even if Razorpay
 * credentials have not been configured yet.
 *
 * @Profile("!test"):
 * The real Razorpay implementation is excluded when the "test"
 * Spring profile is active. Tests use StubPaymentGatewayClient instead.
 */
@Component
@Profile("!test")
public class RazorpayGatewayClientImpl implements PaymentGatewayClient {

    private final String keyId;
    private final String keySecret;

    /**
     * Volatile is required because the client is lazily initialized
     * using double-checked locking.
     */
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

            /*
             * Razorpay expects the amount in the smallest currency unit.
             *
             * Example:
             *
             *     ₹500.00
             *        ↓
             *     50000 paise
             */
            orderRequest.put(
                    "amount",
                    amount.multiply(BigDecimal.valueOf(100)).intValue()
            );

            orderRequest.put("currency", currency);
            orderRequest.put("receipt", receipt);

            /*
             * IMPORTANT:
             *
             * The Razorpay Java SDK exposes the Orders API as
             * lowercase "orders", not "Orders".
             *
             * This is:
             *
             *     getOrCreateClient().orders.create(...)
             *
             * NOT:
             *
             *     getOrCreateClient().Orders.create(...)
             */
            com.razorpay.Order razorpayOrder =
                    getOrCreateClient()
                            .orders
                            .create(orderRequest);

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

    /**
     * Double-checked lazy initialization.
     *
     * The RazorpayClient is:
     *
     * 1. Not created when the Spring bean starts.
     * 2. Created only on the first real payment request.
     * 3. Created at most once.
     * 4. Reused for subsequent payment requests.
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