package com.nexora.payment.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.auth.security.UserPrincipal;
import com.nexora.exception.ForbiddenOperationException;
import com.nexora.exception.ResourceNotFoundException;
import com.nexora.idempotency.service.IdempotencyService;
import com.nexora.inventory.service.InventoryReservationService;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * WHERE PAYMENT INITIATION, VERIFICATION, AND RELIABILITY LOGIC LIVES.
 *
 * PHASE 10: Cryptographic HMAC signature verification & webhook idempotency.
 * PHASE 11: Idempotency-Key support, retry eligibility, and gateway refunds.
 * NEXORA 3.0 PHASE 1:
 *   - Two-phase inventory reservation commitment on verified payment.
 *   - Asynchronous `refund.processed` and `refund.failed` webhook verification.
 *   - Item-level and custom-amount partial refund engine (refundPaymentPartial).
 */
@Slf4j
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

    @Autowired(required = false)
    private InventoryReservationService inventoryReservationService;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    private static final String IDEMPOTENCY_SCOPE_PAYMENT_INITIATE = "PAYMENT_INITIATE";

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
                .refundedAmount(BigDecimal.ZERO)
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

        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            orderService.markAwaitingPayment(order);
        }

        return PaymentMapper.toResponse(savedPayment, razorpayKeyId);
    }

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

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            applySuccessfulPayment(payment, request.getRazorpayPaymentId(),
                    PaymentEventType.CLIENT_SIGNATURE_VERIFIED,
                    "Client-callback signature verified successfully");
        }

        return PaymentMapper.toResponse(payment, razorpayKeyId);
    }

    /**
     * Authoritative Webhook Event Handler:
     * Validates HMAC-SHA256 signature and processes payment capture, failures, and refunds.
     */
    @Transactional
    public void handleWebhookEvent(String rawRequestBody, String signatureHeader) {
        boolean signatureValid = signatureVerifier.verifyWebhookSignature(rawRequestBody, signatureHeader);

        if (!signatureValid) {
            throw new InvalidPaymentSignatureException("Webhook signature verification failed");
        }

        JsonNode root = parseWebhookBody(rawRequestBody);
        String event = root.path("event").asText(null);

        if (event == null) {
            return;
        }

        // 1. Process asynchronous Refund Webhooks
        if ("refund.processed".equals(event)) {
            handleRefundProcessedWebhook(root);
            return;
        } else if ("refund.failed".equals(event)) {
            handleRefundFailedWebhook(root);
            return;
        }

        // 2. Process Payment Webhooks
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

    private void handleRefundProcessedWebhook(JsonNode root) {
        JsonNode refundNode = root.path("payload").path("refund").path("entity");
        String razorpayPaymentId = refundNode.path("payment_id").asText(null);
        String razorpayRefundId = refundNode.path("id").asText(null);
        int amountInPaise = refundNode.path("amount").asInt(0);
        BigDecimal refundAmount = BigDecimal.valueOf(amountInPaise).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        if (razorpayPaymentId == null) {
            return;
        }

        paymentRepository.findByRazorpayPaymentId(razorpayPaymentId).ifPresent(payment -> {
            if (isDuplicateWebhookEvent(payment, PaymentEventType.WEBHOOK_REFUND_PROCESSED, razorpayRefundId)) {
                return;
            }
            applyWebhookRefundProcessed(payment, razorpayRefundId, refundAmount);
        });
    }

    private void handleRefundFailedWebhook(JsonNode root) {
        JsonNode refundNode = root.path("payload").path("refund").path("entity");
        String razorpayPaymentId = refundNode.path("payment_id").asText(null);
        String razorpayRefundId = refundNode.path("id").asText(null);

        if (razorpayPaymentId == null) {
            return;
        }

        paymentRepository.findByRazorpayPaymentId(razorpayPaymentId).ifPresent(payment -> {
            paymentTransactionRepository.save(PaymentTransaction.builder()
                    .payment(payment)
                    .eventType(PaymentEventType.WEBHOOK_REFUND_FAILED)
                    .razorpayPaymentId(razorpayPaymentId)
                    .rawPayload("Webhook reported refund failed for refund ID: " + razorpayRefundId)
                    .build());
        });
    }

    /**
     * Full refund: refunds the remaining refundable amount of a payment.
     */
    @Transactional
    public PaymentResponse refundPayment(UserPrincipal caller, Long paymentId) {
        Payment payment = findPaymentOrThrow(paymentId);
        BigDecimal alreadyRefunded = payment.getRefundedAmount() != null ? payment.getRefundedAmount() : BigDecimal.ZERO;
        BigDecimal remainingRefundable = payment.getAmount().subtract(alreadyRefunded);

        if (remainingRefundable.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentStateException("Payment #" + paymentId + " has already been refunded in full.");
        }

        return refundPaymentPartial(caller, paymentId, remainingRefundable, "Full order refund");
    }

    /**
     * Partial refund: refunds a specified portion of a payment.
     */
    @Transactional
    public PaymentResponse refundPaymentPartial(
            UserPrincipal caller, Long paymentId, BigDecimal partialAmount, String reason) {

        Payment payment = findPaymentOrThrow(paymentId);
        Order order = payment.getOrder();
        assertCallerManagesOrderOrIsAdmin(order, caller);

        if (payment.getStatus() != PaymentStatus.SUCCESS && payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new InvalidPaymentStateException(
                    "Only a SUCCESS or PARTIALLY_REFUNDED payment can be refunded (current status: " + payment.getStatus() + ")");
        }

        if (partialAmount == null || partialAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Refund amount must be strictly greater than zero");
        }

        BigDecimal alreadyRefunded = payment.getRefundedAmount() != null ? payment.getRefundedAmount() : BigDecimal.ZERO;
        BigDecimal remainingRefundable = payment.getAmount().subtract(alreadyRefunded);

        if (partialAmount.compareTo(remainingRefundable) > 0) {
            throw new IllegalArgumentException("Cannot refund " + partialAmount + "; max refundable remaining is " + remainingRefundable);
        }

        RazorpayRefundResult refundResult =
                gatewayClient.refundPayment(payment.getRazorpayPaymentId(), partialAmount);

        BigDecimal newRefundedTotal = alreadyRefunded.add(partialAmount);
        payment.setRefundedAmount(newRefundedTotal);

        boolean isFullRefund = newRefundedTotal.compareTo(payment.getAmount()) >= 0;
        payment.setStatus(isFullRefund ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);
        paymentRepository.save(payment);

        paymentTransactionRepository.save(PaymentTransaction.builder()
                .payment(payment)
                .eventType(isFullRefund ? PaymentEventType.REFUND_COMPLETED : PaymentEventType.PARTIAL_REFUND_PROCESSED)
                .razorpayPaymentId(payment.getRazorpayPaymentId())
                .rawPayload("Refund of " + partialAmount + " processed: " + refundResult.getRazorpayRefundId() +
                        " (Reason: " + (reason != null ? reason : "Adjustment") + ")")
                .build());

        if (isFullRefund) {
            orderService.markCancelledDueToRefund(order);
        }

        // Phase 16: Publish RefundCompletedEvent
        try {
            kafkaEventProducer.publishRefundCompleted(RefundCompletedEvent.builder()
                    .refundId(refundResult.getRazorpayRefundId())
                    .paymentId(payment.getId())
                    .orderId(order.getId())
                    .amount(partialAmount)
                    .reason(reason != null ? reason : "Refund processed for order #" + order.getId())
                    .refundedAt(LocalDateTime.now())
                    .build());
        } catch (Exception ex) {
            // Asynchronous event dispatch failure does not roll back transaction
        }

        return PaymentMapper.toResponse(payment, razorpayKeyId);
    }

    private void applyWebhookRefundProcessed(Payment payment, String razorpayRefundId, BigDecimal refundAmount) {
        BigDecimal alreadyRefunded = payment.getRefundedAmount() != null ? payment.getRefundedAmount() : BigDecimal.ZERO;
        BigDecimal newTotal = alreadyRefunded.add(refundAmount != null ? refundAmount : payment.getAmount());
        payment.setRefundedAmount(newTotal);

        boolean isFullRefund = newTotal.compareTo(payment.getAmount()) >= 0;
        payment.setStatus(isFullRefund ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);
        paymentRepository.save(payment);

        paymentTransactionRepository.save(PaymentTransaction.builder()
                .payment(payment)
                .eventType(PaymentEventType.WEBHOOK_REFUND_PROCESSED)
                .razorpayPaymentId(payment.getRazorpayPaymentId())
                .rawPayload("Webhook confirmed refund processed: " + razorpayRefundId + " (Amount: " + refundAmount + ")")
                .build());

        if (isFullRefund && payment.getOrder() != null) {
            orderService.markCancelledDueToRefund(payment.getOrder());
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

    private void applySuccessfulPayment(
            Payment payment, String razorpayPaymentId, PaymentEventType eventType, String note) {

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setRazorpayPaymentId(razorpayPaymentId);
        paymentRepository.save(payment);

        paymentTransactionRepository.save(PaymentTransaction.builder()
                .payment(payment)
                .eventType(eventType)
                .razorpayPaymentId(razorpayPaymentId)
                .rawPayload(note)
                .build());

        orderService.markPaid(payment.getOrder());

        // Phase 1 Nexora 3.0: Commit Two-Phase Inventory Reservations
        if (inventoryReservationService != null && payment.getOrder() != null) {
            inventoryReservationService.commitReservationsForOrder(payment.getOrder().getId());
        }

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
    }

    private boolean isDuplicateWebhookEvent(Payment payment, PaymentEventType eventType, String identifier) {
        return paymentTransactionRepository.findByPaymentIdOrderByCreatedAtAsc(payment.getId()).stream()
                .anyMatch(tx -> tx.getEventType() == eventType
                        && (Objects.equals(tx.getRazorpayPaymentId(), identifier) ||
                            (tx.getRawPayload() != null && tx.getRawPayload().contains(identifier))));
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
                .anyMatch(role -> role.getName() == RoleName.ADMIN || role.getName() == RoleName.SUPER_ADMIN);
    }

    private void assertCallerOwnsOrderOrIsAdmin(Order order, UserPrincipal caller) {
        if (isAdmin(caller)) {
            return;
        }
        if (!order.getUser().getId().equals(caller.getId())) {
            throw new ForbiddenOperationException("You can only manage payments for your own orders.");
        }
    }

    private void assertCallerManagesOrderOrIsAdmin(Order order, UserPrincipal caller) {
        if (isAdmin(caller)) {
            return;
        }
        if (order.getStore() != null && order.getStore().getOwner() != null &&
                order.getStore().getOwner().getId().equals(caller.getId())) {
            return;
        }
        throw new ForbiddenOperationException("You do not have permission to refund payments for this order.");
    }

    private void assertCallerCanViewPayment(Payment payment, UserPrincipal caller) {
        assertCallerCanViewOrderPayments(payment.getOrder(), caller);
    }

    private void assertCallerCanViewOrderPayments(Order order, UserPrincipal caller) {
        if (isAdmin(caller)) {
            return;
        }
        boolean isOwningCustomer = order.getUser() != null && order.getUser().getId().equals(caller.getId());
        boolean isManagingStoreOwner = order.getStore() != null && order.getStore().getOwner() != null &&
                order.getStore().getOwner().getId().equals(caller.getId());
        if (!isOwningCustomer && !isManagingStoreOwner) {
            throw new ForbiddenOperationException("You do not have permission to view this payment.");
        }
    }
}
