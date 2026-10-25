package com.nexora.testsupport;

import com.nexora.payment.gateway.PaymentGatewayClient;
import com.nexora.payment.gateway.RazorpayOrderResult;
import com.nexora.payment.gateway.RazorpayRefundResult;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * ACTIVE ONLY under the "test" Spring profile (activated for the
 * whole suite by src/test/resources/application.properties), and
 * takes the place of RazorpayGatewayClientImpl (@Profile("!test")) —
 * see that class's javadoc for the full reasoning.
 *
 * Returns an instant, deterministic, fake result — no network call,
 * no real credentials needed. Individual tests that need specific
 * gateway behavior (e.g. simulating a gateway outage) can still
 * override this further with @MockBean, which always takes priority
 * over a regular @Component bean.
 */
@Component
@Profile("test")
public class StubPaymentGatewayClient implements PaymentGatewayClient {

    @Override
    public RazorpayOrderResult createOrder(BigDecimal amount, String currency, String receipt) {
        String fakeRazorpayOrderId = "order_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        return new RazorpayOrderResult(fakeRazorpayOrderId, "created");
    }

    @Override
    public RazorpayRefundResult refundPayment(String razorpayPaymentId, BigDecimal amount) {
        String fakeRefundId = "rfnd_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        return new RazorpayRefundResult(fakeRefundId, "processed");
    }
}
