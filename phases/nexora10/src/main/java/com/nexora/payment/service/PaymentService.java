package com.nexora.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.nexora.payment.exception.InvalidPaymentSignatureException;
import com.nexora.payment.exception.InvalidPaymentStateException;
import com.nexora.payment.gateway.PaymentGatewayClient;
import com.nexora.payment.gateway.RazorpayOrderResult;
import com.nexora.payment.mapper.PaymentMapper;
import com.nexora.payment.repository.PaymentRepository;
import com.nexora.payment.repository.PaymentTransactionRepository;
import com.nexora.payment.security.RazorpaySignatureVerifier;
import com.nexora.role.entity.RoleName;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * WHERE PAYMENT INITIATION *AND VERIFICATION* LOGIC LIVES.
 *
 * PHASE 9 built initiation and an unverified client-result logger.
 * PHASE 10 closes the loop: a Payment can now genuinely reach SUCCESS,
 * and an Order can genuinely reach PAID — but ONLY through
 * applySuccessfulPayment(), which is ONLY ever called after a
 * signature has been cryptographically verified, via one of two paths:
 *
 *   1. recordClientReportedResult() — verifies the CLIENT-CALLBACK
 *      signature (RazorpaySignatureVerifier.verifyPaymentSignature),
 *      computed with razorpay.key-secret. If it doesn't match, the
 *      call is REJECTED (400 InvalidPaymentSignatureException) — the
 *      payment status is untouched either way; there is no "silently
 *      ignore and stay PENDING" path anymore, an invalid signature is
 *      now treated as the security-relevant event it is.
 *
 *   2. handleWebhookEvent() — verifies the WEBHOOK signature (
 *      RazorpaySignatureVerifier.verifyWebhookSignature), computed
 *      with razorpay.webhook-secret, over the exact raw request body
 *      Razorpay sent. This is the MORE RESILIENT path: it doesn't
 *      depend on the customer's browser successfully calling back at
 *      all, so it's what a production deployment should treat as
 *      authoritative even if the client-callback path also fires.
 *
 * Both paths are equally legitimate cryptographic proof that the data
 * genuinely came from Razorpay — neither is "trusting the frontend."
 * What Phase 9 correctly refused to do, and what would still be
 * wrong, is accepting an UNVERIFIED claim. Verified is not the same
 * as client-reported.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OrderService orderService;
    private final PaymentGatewayClient gatewayClient;
    private final RazorpaySignatureVerifier signatureVerifier;
    private final ObjectMapper objectMapper;

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
     * PHASE 10: verifies the client-callback signature and, if valid,
     * genuinely confirms the payment — see this class's javadoc. If
     * the signature doesn't match, this THROWS rather than silently
     * doing nothing: an invalid signature here means either the data
     * was tampered with in transit, or someone is attempting to spoof
     * a payment confirmation without ever having paid.
     */
    @Transactional
    public PaymentResponse recordClientReportedResult(
            UserPrincipal caller, Long paymentId, PaymentResultRequest request) {

        Payment payment = findPaymentOrThrow(paymentId);
        assertCallerOwnsOrderOrIsAdmin(payment.getOrder(), caller);

        boolean signatureValid = signatureVerifier.verifyPaymentSignature(
                payment.getRazorpayOrderId(), request.getRazorpayPaymentId(), request.getRazorpaySignature());

        if (!signatureValid) {
            paymentTransactionRepository.save(PaymentTransaction.builder()
                    .payment(payment)
                    .eventType(PaymentEventType.CLIENT_SIGNATURE_INVALID)
                    .razorpayPaymentId(request.getRazorpayPaymentId())
                    .rawPayload("Client-reported signature failed verification — rejected")
                    .build());
            throw new InvalidPaymentSignatureException(
                    "Payment signature verification failed for payment " + paymentId);
        }

        // Idempotent: if a webhook already confirmed this same payment,
        // don't reprocess — just report the (already-correct) current state.
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            applySuccessfulPayment(payment, request.getRazorpayPaymentId(),
                    PaymentEventType.CLIENT_SIGNATURE_VERIFIED,
                    "Client-callback signature verified successfully");
        }

        return PaymentMapper.toResponse(payment, razorpayKeyId);
    }

    /**
     * PHASE 10: handles an incoming Razorpay webhook call. This method
     * has no caller/authorization check at all — by design. Webhooks
     * come from Razorpay's servers directly, with no Nexora user or
     * JWT involved; the ENTIRE security model for this endpoint is the
     * signature check below (see PaymentWebhookController, and
     * SecurityConfig's permitAll list). A request with a missing or
     * incorrect signature is rejected before any payload data is
     * trusted for anything.
     *
     * @param rawRequestBody the EXACT, untouched raw JSON body Razorpay
     *                       sent — required because the signature is
     *                       computed over those exact bytes; parsing
     *                       and re-serializing first (even to the
     *                       "same" JSON) could change whitespace/key
     *                       order and silently break verification.
     * @param signatureHeader the X-Razorpay-Signature header value
     */
    @Transactional
    public void handleWebhookEvent(String rawRequestBody, String signatureHeader) {
        boolean signatureValid = signatureVerifier.verifyWebhookSignature(rawRequestBody, signatureHeader);

        if (!signatureValid) {
            // We deliberately do NOT parse/act on the payload at all in
            // this branch — an unverified body could claim to be about
            // ANY order, and acting on it before verification is exactly
            // what "do not trust frontend/unverified payment messages"
            // forbids. We simply reject.
            throw new InvalidPaymentSignatureException("Webhook signature verification failed");
        }

        JsonNode root = parseWebhookBody(rawRequestBody);
        String event = root.path("event").asText(null);
        String razorpayOrderId = root.path("payload").path("payment").path("entity").path("order_id").asText(null);
        String razorpayPaymentId = root.path("payload").path("payment").path("entity").path("id").asText(null);

        if (razorpayOrderId == null) {
            throw new ResourceNotFoundException("Webhook payload is missing an order_id");
        }

        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No payment found for Razorpay order: " + razorpayOrderId));

        PaymentEventType targetEventType = mapToEventType(event);
        if (targetEventType == null) {
            // A real webhook event we don't act on (e.g. "order.paid",
            // refund events reserved for Phase 11). Signature was valid,
            // so this is a legitimate call — we simply have nothing to
            // do with it; returning normally acknowledges it (2xx) so
            // Razorpay doesn't retry.
            return;
        }

        if (isDuplicateWebhookEvent(payment, targetEventType, razorpayPaymentId)) {
            paymentTransactionRepository.save(PaymentTransaction.builder()
                    .payment(payment)
                    .eventType(PaymentEventType.WEBHOOK_DUPLICATE_IGNORED)
                    .razorpayPaymentId(razorpayPaymentId)
                    .rawPayload("Duplicate '" + event + "' webhook ignored — already processed")
                    .build());
            return;
        }

        if (targetEventType == PaymentEventType.WEBHOOK_PAYMENT_CAPTURED) {
            applySuccessfulPayment(payment, razorpayPaymentId, PaymentEventType.WEBHOOK_PAYMENT_CAPTURED,
                    "Webhook confirmed payment captured (event: " + event + ")");
        } else {
            applyFailedPayment(payment, razorpayPaymentId, event);
        }
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

    /**
     * THE ONE PLACE a Payment ever becomes SUCCESS and an Order ever
     * becomes PAID. Called only after signature verification has
     * already succeeded, by either recordClientReportedResult() or
     * handleWebhookEvent().
     */
    private void applySuccessfulPayment(
            Payment payment, String razorpayPaymentId, PaymentEventType eventType, String note) {

        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        paymentTransactionRepository.save(PaymentTransaction.builder()
                .payment(payment)
                .eventType(eventType)
                .razorpayPaymentId(razorpayPaymentId)
                .rawPayload(note)
                .build());

        // markPaid() is itself idempotent-safe (Phase 10) — see its javadoc.
        orderService.markPaid(payment.getOrder());
    }

    private void applyFailedPayment(Payment payment, String razorpayPaymentId, String event) {
        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);

        paymentTransactionRepository.save(PaymentTransaction.builder()
                .payment(payment)
                .eventType(PaymentEventType.WEBHOOK_PAYMENT_FAILED)
                .razorpayPaymentId(razorpayPaymentId)
                .rawPayload("Webhook reported payment failed (event: " + event + ")")
                .build());

        // Deliberately does NOT auto-cancel the order — a failed
        // payment attempt just means the customer can retry via a
        // fresh POST /orders/{id}/payments; the order itself stays
        // PAYMENT_PENDING. Auto-cancelling on the first failure would
        // be unnecessarily punishing for something as routine as a
        // declined card that the customer can immediately retry.
    }

    private boolean isDuplicateWebhookEvent(Payment payment, PaymentEventType eventType, String razorpayPaymentId) {
        return paymentTransactionRepository.findByPaymentIdOrderByCreatedAtAsc(payment.getId()).stream()
                .anyMatch(tx -> tx.getEventType() == eventType
                        && Objects.equals(tx.getRazorpayPaymentId(), razorpayPaymentId));
    }

    private PaymentEventType mapToEventType(String razorpayEvent) {
        if ("payment.captured".equals(razorpayEvent)) {
            return PaymentEventType.WEBHOOK_PAYMENT_CAPTURED;
        }
        if ("payment.failed".equals(razorpayEvent)) {
            return PaymentEventType.WEBHOOK_PAYMENT_FAILED;
        }
        return null;
    }

    private JsonNode parseWebhookBody(String rawRequestBody) {
        try {
            return objectMapper.readTree(rawRequestBody);
        } catch (Exception ex) {
            throw new InvalidPaymentSignatureException("Webhook payload could not be parsed as JSON");
        }
    }

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
