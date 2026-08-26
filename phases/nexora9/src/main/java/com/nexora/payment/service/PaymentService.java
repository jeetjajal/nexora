package com.nexora.payment.service;

import com.nexora.auth.security.UserPrincipal;
import com.nexora.exception.ForbiddenOperationException;
import com.nexora.exception.ResourceNotFoundException;
import com.nexora.order.entity.Order;
import com.nexora.order.entity.OrderStatus;
import com.nexora.order.service.OrderService;
import com.nexora.payment.dto.PaymentResponse;
import com.nexora.payment.dto.PaymentResultRequest;
import com.nexora.payment.entity.Payment;
import com.nexora.payment.entity.PaymentEventType;
import com.nexora.payment.entity.PaymentStatus;
import com.nexora.payment.entity.PaymentTransaction;
import com.nexora.payment.exception.InvalidPaymentStateException;
import com.nexora.payment.gateway.PaymentGatewayClient;
import com.nexora.payment.gateway.RazorpayOrderResult;
import com.nexora.payment.mapper.PaymentMapper;
import com.nexora.payment.repository.PaymentRepository;
import com.nexora.payment.repository.PaymentTransactionRepository;
import com.nexora.role.entity.RoleName;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * WHERE PAYMENT-INITIATION LOGIC LIVES.
 *
 * THE MOST IMPORTANT RULE IN THIS CLASS: recordClientReportedResult()
 * NEVER sets a Payment's status to SUCCESS. Razorpay's Checkout widget
 * runs entirely in the customer's browser; by the time our frontend
 * calls us back with a "payment_id" and "signature," that data came
 * from JavaScript running on a device we don't control. A malicious or
 * compromised client could call this endpoint with a fabricated
 * payment id and claim success without ever having paid a single
 * rupee. Trusting it would let anyone place free orders.
 *
 * The ONLY thing this phase does with a client-reported result is log
 * it — for audit, and so a human (or Phase 10's webhook handler) has
 * a record of what the client claimed happened. The signature Razorpay
 * attaches to that callback IS cryptographically meaningful and CAN be
 * verified — but that verification, and the resulting Payment/Order
 * status update, is explicitly Phase 10's job (real webhook handling,
 * "do not trust frontend payment-success messages"). Until then, a
 * payment in Nexora only ever reaches PENDING through this phase —
 * getting to SUCCESS requires Phase 10.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OrderService orderService;
    private final PaymentGatewayClient gatewayClient;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    /**
     * Initiates payment for an order: asks the gateway for a Razorpay
     * order, stores a Payment row, logs an ORDER_CREATED transaction,
     * and moves the Nexora order from CREATED to PAYMENT_PENDING.
     *
     * If a PENDING payment already exists for this order (e.g. the
     * customer refreshed the checkout page), that SAME payment is
     * returned instead of creating a duplicate Razorpay order — a
     * small, deliberate step toward the idempotency Phase 11 builds
     * out fully.
     */
    @Transactional
    public PaymentResponse initiatePayment(UserPrincipal caller, Long orderId) {
        Order order = orderService.getOrderEntityOrThrow(orderId);
        assertCallerOwnsOrderOrIsAdmin(order, caller);

        List<Payment> existingPayments = paymentRepository.findByOrderId(orderId);

        boolean alreadyPaid = existingPayments.stream()
                .anyMatch(p -> p.getStatus() == PaymentStatus.SUCCESS);
        if (alreadyPaid) {
            throw new InvalidPaymentStateException("Order " + orderId + " has already been paid.");
        }

        Payment existingPending = existingPayments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PENDING || p.getStatus() == PaymentStatus.CREATED)
                .findFirst()
                .orElse(null);
        if (existingPending != null) {
            return PaymentMapper.toResponse(existingPending, razorpayKeyId);
        }

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new InvalidPaymentStateException(
                    "Cannot initiate payment for an order in status " + order.getStatus() +
                            " (expected CREATED)");
        }

        String receipt = "order_" + order.getId() + "_" + System.currentTimeMillis();
        RazorpayOrderResult gatewayResult = gatewayClient.createOrder(order.getTotalAmount(), "INR", receipt);

        Payment payment = Payment.builder()
                .order(order)
                .razorpayOrderId(gatewayResult.getRazorpayOrderId())
                .amount(order.getTotalAmount())
                .currency("INR")
                .status(PaymentStatus.PENDING)
                .gateway("RAZORPAY")
                .build();
        Payment savedPayment = paymentRepository.save(payment);

        paymentTransactionRepository.save(PaymentTransaction.builder()
                .payment(savedPayment)
                .eventType(PaymentEventType.ORDER_CREATED)
                .rawPayload("Razorpay order created: " + gatewayResult.getRazorpayOrderId() +
                        " (gateway status: " + gatewayResult.getStatus() + ")")
                .build());

        orderService.markAwaitingPayment(order);

        return PaymentMapper.toResponse(savedPayment, razorpayKeyId);
    }

    /**
     * Records what the frontend reported after the Checkout widget
     * closed. See this class's javadoc — this NEVER marks the payment
     * SUCCESS on its own.
     */
    @Transactional
    public PaymentResponse recordClientReportedResult(
            UserPrincipal caller, Long paymentId, PaymentResultRequest request) {

        Payment payment = findPaymentOrThrow(paymentId);
        assertCallerOwnsOrderOrIsAdmin(payment.getOrder(), caller);

        paymentTransactionRepository.save(PaymentTransaction.builder()
                .payment(payment)
                .eventType(PaymentEventType.CLIENT_REPORTED_RESULT)
                .razorpayPaymentId(request.getRazorpayPaymentId())
                .rawPayload("Client reported razorpay_payment_id=" + request.getRazorpayPaymentId() +
                        " — UNVERIFIED, awaiting webhook signature check (Phase 10)")
                .build());

        return PaymentMapper.toResponse(payment, razorpayKeyId);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(UserPrincipal caller, Long paymentId) {
        Payment payment = findPaymentOrThrow(paymentId);
        assertCallerCanViewPayment(payment, caller);
        return PaymentMapper.toResponse(payment, razorpayKeyId);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsForOrder(UserPrincipal caller, Long orderId) {
        Order order = orderService.getOrderEntityOrThrow(orderId);
        assertCallerCanViewOrderPayments(order, caller);

        return paymentRepository.findByOrderId(orderId).stream()
                .map(p -> PaymentMapper.toResponse(p, razorpayKeyId))
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------

    private Payment findPaymentOrThrow(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));
    }

    private boolean isAdmin(UserPrincipal caller) {
        return caller.getUser().getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ADMIN);
    }

    /** Only the customer who placed the order (or ADMIN) may initiate/report payment for it. */
    private void assertCallerOwnsOrderOrIsAdmin(Order order, UserPrincipal caller) {
        if (isAdmin(caller)) {
            return;
        }
        if (!order.getUser().getId().equals(caller.getId())) {
            throw new ForbiddenOperationException("You can only manage payments for your own orders.");
        }
    }

    /** Viewing a single payment: owning customer, the order's store owner, or ADMIN. */
    private void assertCallerCanViewPayment(Payment payment, UserPrincipal caller) {
        assertCallerCanViewOrderPayments(payment.getOrder(), caller);
    }

    private void assertCallerCanViewOrderPayments(Order order, UserPrincipal caller) {
        if (isAdmin(caller)) {
            return;
        }
        boolean isOwningCustomer = order.getUser().getId().equals(caller.getId());
        boolean isManagingStoreOwner = order.getStore().getOwner().getId().equals(caller.getId());
        if (!isOwningCustomer && !isManagingStoreOwner) {
            throw new ForbiddenOperationException("You do not have permission to view this payment.");
        }
    }
}
