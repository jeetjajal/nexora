package com.nexora.payment.gateway;

import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * THE ONLY CLASS IN THIS ENTIRE CODEBASE THAT IMPORTS com.razorpay.*.
 * Everything else depends on the PaymentGatewayClient interface.
 *
 * CREDENTIALS: read from environment variables, never hard-coded.
 *
 * AMOUNT UNITS: Razorpay expects the smallest currency unit.
 * For INR, this means paise, so rupees are multiplied by 100.
 *
 * LAZY CLIENT CONSTRUCTION: the real RazorpayClient is created only
 * when createOrder() is actually called.
 *
 * @Profile("!test"): excluded when the "test" Spring profile is active.
 * The test suite uses StubPaymentGatewayClient instead.
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

            // Convert INR rupees to paise.
            orderRequest.put(
                    "amount",
                    amount.multiply(BigDecimal.valueOf(100)).intValue()
            );

            orderRequest.put("currency", currency);
            orderRequest.put("receipt", receipt);

            /*
             * IMPORTANT:
             * Razorpay Java SDK exposes the orders API as lowercase
             * "orders", not "Orders".
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
     * The RazorpayClient is created at most once, on the first
     * real payment request, and reused afterward.
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