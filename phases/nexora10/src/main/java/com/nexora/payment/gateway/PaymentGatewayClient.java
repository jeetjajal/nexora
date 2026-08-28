package com.nexora.payment.gateway;

import java.math.BigDecimal;

/**
 * WHY THIS INTERFACE EXISTS INSTEAD OF PaymentService CALLING THE
 * RAZORPAY SDK DIRECTLY:
 *
 * 1. TESTABILITY. Our test suite must never make real network calls
 *    to Razorpay's servers — that would make tests slow, flaky
 *    (dependent on network/Razorpay uptime), and would require every
 *    contributor to have real Razorpay test credentials just to run
 *    `mvn test`. By depending on this interface, PaymentServiceTest
 *    and the Phase 9 integration test can substitute a fake/mock
 *    implementation that returns an instant, predictable result —
 *    while PaymentService's actual logic (persistence, status
 *    handling, authorization, the order-state transition) is
 *    exercised for real.
 *
 * 2. ISOLATION. If Nexora ever needed to support a second payment
 *    gateway alongside Razorpay, only a new implementation of this
 *    interface would be needed — nothing in PaymentService, the
 *    controller, or the database schema would have to change.
 *
 * The real implementation, RazorpayGatewayClientImpl, wraps the
 * official `com.razorpay:razorpay-java` SDK.
 */
public interface PaymentGatewayClient {

    /**
     * Asks the gateway to create a new payment order.
     *
     * @param amount   the exact amount to charge, in rupees (this
     *                 method is responsible for converting to paise —
     *                 Razorpay's smallest currency unit — internally)
     * @param currency ISO currency code, e.g. "INR"
     * @param receipt  our own reference string (e.g. "order_42_...")
     *                 so we can recognize this order in Razorpay's
     *                 dashboard later
     */
    RazorpayOrderResult createOrder(BigDecimal amount, String currency, String receipt);
}
