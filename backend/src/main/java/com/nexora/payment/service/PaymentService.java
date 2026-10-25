package com.nexora.payment.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.auth.security.UserPrincipal;
import com.nexora.exception.ForbiddenOperationException;
import com.nexora.exception.ResourceNotFoundException;
import com.nexora.idempotency.service.IdempotencyService;
import com.nexora.kafka.event.PaymentCompletedEvent;
import com.nexora.kafka.event.PaymentFailedEvent;
import com.nexora.kafka.event.RefundCompletedEvent;
import com.nexora.kafka.producer.KafkaEventProducer;
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
import com.nexora.payment.gateway.RazorpayRefundResult;
import com.nexora.payment.mapper.PaymentMapper;
import com.nexora.payment.repository.PaymentRepository;
import com.nexora.payment.repository.PaymentTransactionRepository;
import com.nexora.payment.security.RazorpaySignatureVerifier;
import com.nexora.role.entity.RoleName;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * WHERE PAYMENT INITIATION, VERIFICATION, AND RELIABILITY LOGIC LIVES.
 *
 * PHASE 9 built initiation and an unverified client-result logger.
 * PHASE 10 added real cryptographic verification (applySuccessfulPayment
 * is the only path to Payment.SUCCESS / Order.PAID).
 * PHASE 11 (this phase) adds:
 *   - A genuine RETRY path: initiatePayment now allows a fresh attempt
 *     from PAYMENT_PENDING (not just CREATED) — see initiatePayment's
 *     javadoc for why the Phase 9/10 version had a real gap here.
 *   - Formal Idempotency-Key support (initiatePaymentIdempotent) — see
 *     IdempotencyKey's javadoc for the full "network fails, client
 *     retries" scenario this solves.
 *   - A refund flow (refundPayment), using the REFUNDED PaymentStatus
 *     value that's existed since Phase 9 but had no code path to it.
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
    private final IdempotencyService idempotencyService;
    private final KafkaEventProducer kafkaEventProducer;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    private static final String IDEMPOTENCY_SCOPE_PAYMENT_INITIATE = "PAYMENT_INITIATE";

    /**
     * PHASE 11: the Idempotency-Key-aware entry point. If a key is
     * supplied, wraps the real operation (initiatePayment below) so a
     * retried request with the SAME key never re-runs it — see
     * IdempotencyService's javadoc. If no key is supplied (null/blank),
     * falls straight through to the plain method with no tracking at
     * all — existing callers that don't send the header see
     * byte-for-byte the same behavior as before this phase.
     */
    public PaymentResponse initiatePayment(UserPrincipal caller, Long orderId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return initiatePayment(caller, orderId);
        }

        String fingerprint = String.valueOf(orderId);
        return idempotencyService.executeIdempotently(
                idempotencyKey,
                IDEMPOTENCY_SCOPE_PAYMENT_INITIATE,
                caller.getId(),
                fingerprint,
                new TypeReference<PaymentResponse>() {},
                () -> initiatePayment(caller, orderId));
    }

    /**
     * Initiates payment for an order: asks the gateway for a Razorpay
     * order, stores a Payment row, logs an ORDER_CREATED transaction,
     * and moves the Nexora order from CREATED to PAYMENT_PENDING.
     *
     * ELIGIBILITY (PHASE 11 FIX): a new attempt is allowed when the
     * order is CREATED **or** PAYMENT_PENDING, as long as no payment is
     * currently SUCCESS or actively PENDING/CREATED. The Phase 9/10
     * version only allowed CREATED — but Phase 10's webhook failure
     * handling deliberately leaves a failed order at PAYMENT_PENDING
     * (see PaymentService.applyFailedPayment's comment), not back at
     * CREATED. That meant a customer whose card was declined had NO
     * way to retry: initiatePayment would reject every subsequent
     * attempt with "expected CREATED." This is precisely the
     * "implement payment retry" requirement this phase exists for.
     *
     * If a PENDING/CREATED payment already exists for this order (e.g.
     * the customer refreshed the checkout page mid-flight), that SAME
     * payment is returned instead of creating a duplicate Razorpay
     * order — a lighter-weight companion to the formal Idempotency-Key
     * mechanism above, catching the common case even when no key was sent.
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

        boolean eligibleForNewAttempt =
                order.getStatus() == OrderStatus.CREATED || order.getStatus() == OrderStatus.PAYMENT_PENDING;
        if (!eligibleForNewAttempt) {
            throw new InvalidPaymentStateException(
                    "Cannot initiate payment for an order in status " + order.getStatus() +
                            " (expected CREATED or PAYMENT_PENDING)");
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

        // Only actually transitions if not already PAYMENT_PENDING
        // (retry case) — markAwaitingPayment's underlying state-machine
        // check treats PAYMENT_PENDING -> PAYMENT_PENDING as a no-op
        // target it doesn't need to re-apply; see OrderService.
        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            orderService.markAwaitingPayment(order);
        }

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
            // A real webhook event we don't act on (e.g. refund.processed
            // webhooks — Phase 11's refund flow is synchronous/gateway-
            // response-driven rather than webhook-confirmed; see
            // refundPayment's javadoc for that scope boundary).
            // Signature was valid, so this is a legitimate call — we
            // simply have nothing to do with it; returning normally
            // acknowledges it (2xx) so Razorpay doesn't retry.
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

    /**
     * PHASE 11: refunds a SUCCESS payment in full, via the gateway's
     * refund API, and marks the order CANCELLED as a consequence (see
     * OrderService.markCancelledDueToRefund — idempotent-safe, and
     * forgiving of an order that's already in a terminal state).
     *
     * AUTHORIZATION: deliberately restricted to the order's STORE_OWNER
     * or ADMIN — NOT the customer. Refund decisions move real money
     * back out; the same "never trust the frontend, backend stays
     * authoritative" philosophy that's run through this whole project
     * (never trusting a client-supplied price, a client-reported
     * payment success, etc.) applies here too: a customer cannot
     * unilaterally trigger a refund through this API. A customer who
     * wants a refund goes through their store/support, same as most
     * real platforms.
     *
     * SCOPE BOUNDARY: this is a full refund only (no partial refunds),
     * and its outcome is trusted directly from the gateway API's
     * synchronous response rather than confirmed asynchronously via a
     * refund.processed webhook. A production system would likely also
     * listen for that webhook as the authoritative confirmation (the
     * same reasoning Phase 10 applied to payments); that's a natural
     * next increment, not implemented here to keep this phase focused.
     */
    @Transactional
    public PaymentResponse refundPayment(UserPrincipal caller, Long paymentId) {
        Payment payment = findPaymentOrThrow(paymentId);
        Order order = payment.getOrder();
        assertCallerManagesOrderOrIsAdmin(order, caller);

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new InvalidPaymentStateException(
                    "Only a SUCCESS payment can be refunded (current status: " + payment.getStatus() + ")");
        }

        RazorpayRefundResult refundResult =
                gatewayClient.refundPayment(payment.getRazorpayPaymentId(), payment.getAmount());

        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        paymentTransactionRepository.save(PaymentTransaction.builder()
                .payment(payment)
                .eventType(PaymentEventType.REFUND_COMPLETED)
                .razorpayPaymentId(payment.getRazorpayPaymentId())
                .rawPayload("Refund processed: " + refundResult.getRazorpayRefundId() +
                        " (gateway status: " + refundResult.getStatus() + ")")
                .build());

        orderService.markCancelledDueToRefund(order);

        // Phase 16: Publish RefundCompletedEvent
        try {
            kafkaEventProducer.publishRefundCompleted(RefundCompletedEvent.builder()
                    .refundId(refundResult.getRazorpayRefundId())
                    .paymentId(payment.getId())
                    .orderId(order.getId())
                    .amount(payment.getAmount())
                    .reason("Refund processed for order #" + order.getId())
                    .refundedAt(LocalDateTime.now())
                    .build());
        } catch (Exception ex) {
            // Asynchronous event dispatch failure does not roll back transaction
        }

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

    /**
     * THE ONE PLACE a Payment ever becomes SUCCESS and an Order ever
     * becomes PAID. Called only after signature verification has
     * already succeeded, by either recordClientReportedResult() or
     * handleWebhookEvent().
     */
    private void applySuccessfulPayment(
            Payment payment, String razorpayPaymentId, PaymentEventType eventType, String note) {

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setRazorpayPaymentId(razorpayPaymentId); // PHASE 11: needed later for refundPayment
        paymentRepository.save(payment);

        paymentTransactionRepository.save(PaymentTransaction.builder()
                .payment(payment)
                .eventType(eventType)
                .razorpayPaymentId(razorpayPaymentId)
                .rawPayload(note)
                .build());

        // markPaid() is itself idempotent-safe (Phase 10) — see its javadoc.
        orderService.markPaid(payment.getOrder());

        // Phase 16: Publish PaymentCompletedEvent
        try {
            kafkaEventProducer.publishPaymentCompleted(PaymentCompletedEvent.builder()
                    .paymentId(payment.getId())
                    .orderId(payment.getOrder().getId())
                    .razorpayPaymentId(razorpayPaymentId)
                    .razorpayOrderId(payment.getRazorpayOrderId())
                    .amount(payment.getAmount())
                    .paymentMethod(payment.getGateway() != null ? payment.getGateway() : "RAZORPAY")
                    .completedAt(LocalDateTime.now())
                    .build());
        } catch (Exception ex) {
            // Asynchronous event dispatch failure does not block payment
        }
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

        // Phase 16: Publish PaymentFailedEvent
        try {
            kafkaEventProducer.publishPaymentFailed(PaymentFailedEvent.builder()
                    .paymentId(payment.getId())
                    .orderId(payment.getOrder().getId())
                    .errorCode(event)
                    .failureReason("Webhook reported failure: " + event)
                    .amount(payment.getAmount())
                    .failedAt(LocalDateTime.now())
                    .build());
        } catch (Exception ex) {
            // Asynchronous event dispatch failure does not block payment
        }

        // Deliberately does NOT auto-cancel the order — a failed
        // payment attempt just means the customer can retry via a
        // fresh POST /orders/{id}/payments (now genuinely possible as
        // of Phase 11's eligibility fix above); the order itself stays
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

    /** Refund authorization: the order's STORE_OWNER, or ADMIN — never the customer. See refundPayment's javadoc. */
    private void assertCallerManagesOrderOrIsAdmin(Order order, UserPrincipal caller) {
        if (isAdmin(caller)) {
            return;
        }
        if (!order.getStore().getOwner().getId().equals(caller.getId())) {
            throw new ForbiddenOperationException("You do not have permission to refund payments for this order.");
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
