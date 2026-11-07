package com.nexora.phase1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.auth.security.UserPrincipal;
import com.nexora.exception.ForbiddenOperationException;
import com.nexora.idempotency.service.IdempotencyService;
import com.nexora.inventory.service.InventoryReservationService;
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
import com.nexora.payment.exception.InvalidPaymentStateException;
import com.nexora.payment.gateway.PaymentGatewayClient;
import com.nexora.payment.gateway.RazorpayRefundResult;
import com.nexora.payment.repository.PaymentRepository;
import com.nexora.payment.repository.PaymentTransactionRepository;
import com.nexora.payment.security.RazorpaySignatureVerifier;
import com.nexora.payment.service.PaymentService;
import com.nexora.role.entity.Role;
import com.nexora.role.entity.RoleName;
import com.nexora.store.entity.Store;
import com.nexora.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 1: Hardened Payments, Partial Refunds & Webhook Tests")
class PaymentPartialRefundAndWebhookTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private OrderService orderService;
    @Mock private PaymentGatewayClient gatewayClient;
    @Mock private RazorpaySignatureVerifier signatureVerifier;
    @Mock private IdempotencyService idempotencyService;
    @Mock private KafkaEventProducer kafkaEventProducer;
    @Mock private InventoryReservationService inventoryReservationService;

    private PaymentService paymentService;
    private ObjectMapper objectMapper;

    private User adminUser;
    private User customer;
    private Order testOrder;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        paymentService = new PaymentService(
                paymentRepository,
                paymentTransactionRepository,
                orderService,
                gatewayClient,
                signatureVerifier,
                objectMapper,
                idempotencyService,
                kafkaEventProducer
        );

        ReflectionTestUtils.setField(paymentService, "razorpayKeyId", "rzp_test_nexora_phase1");
        ReflectionTestUtils.setField(paymentService, "inventoryReservationService", inventoryReservationService);

        Role adminRole = Role.builder().id(1L).name(RoleName.ADMIN).build();
        Role customerRole = Role.builder().id(2L).name(RoleName.CUSTOMER).build();

        adminUser = User.builder().id(999L).email("admin@nexora.test").roles(Set.of(adminRole)).build();
        customer = User.builder().id(101L).email("customer@nexora.test").roles(Set.of(customerRole)).build();

        Store store = Store.builder().id(1L).name("Ahmedabad Central Hub").build();

        testOrder = Order.builder()
                .id(7001L)
                .user(customer)
                .store(store)
                .status(OrderStatus.CONFIRMED)
                .totalAmount(BigDecimal.valueOf(500.00))
                .build();

        testPayment = Payment.builder()
                .id(9001L)
                .order(testOrder)
                .amount(BigDecimal.valueOf(500.00))
                .refundedAmount(BigDecimal.ZERO)
                .status(PaymentStatus.SUCCESS)
                .gateway("RAZORPAY")
                .razorpayOrderId("order_test_9001")
                .razorpayPaymentId("pay_test_9001")
                .build();
    }

    @Test
    @DisplayName("Workstream 4: Partial refund updates payment status to PARTIALLY_REFUNDED and sets refundedAmount")
    void testPartialRefundUpdatesStatusAndAmount() {
        when(paymentRepository.findById(9001L)).thenReturn(Optional.of(testPayment));
        when(gatewayClient.refundPayment(eq("pay_test_9001"), eq(BigDecimal.valueOf(200.00))))
                .thenReturn(new RazorpayRefundResult("rfnd_test_200", "processed"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        UserPrincipal adminPrincipal = new UserPrincipal(adminUser);
        PaymentResponse response = paymentService.refundPaymentPartial(
                adminPrincipal, 9001L, BigDecimal.valueOf(200.00), "Missing 2 items");

        assertNotNull(response);
        assertEquals(PaymentStatus.PARTIALLY_REFUNDED, testPayment.getStatus());
        assertEquals(BigDecimal.valueOf(200.00), testPayment.getRefundedAmount());

        ArgumentCaptor<PaymentTransaction> txCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).save(txCaptor.capture());
        assertEquals(PaymentEventType.PARTIAL_REFUND_PROCESSED, txCaptor.getValue().getEventType());
        assertTrue(txCaptor.getValue().getRawPayload().contains("rfnd_test_200"));

        // Since it's partial, order is NOT cancelled
        verify(orderService, never()).markCancelledDueToRefund(any());
    }

    @Test
    @DisplayName("Workstream 4: Second partial refund completing total transitions status to REFUNDED and cancels order")
    void testRemainingPartialRefundTransitionsToFullRefunded() {
        testPayment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
        testPayment.setRefundedAmount(BigDecimal.valueOf(200.00));

        when(paymentRepository.findById(9001L)).thenReturn(Optional.of(testPayment));
        when(gatewayClient.refundPayment(eq("pay_test_9001"), eq(BigDecimal.valueOf(300.00))))
                .thenReturn(new RazorpayRefundResult("rfnd_test_300", "processed"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        UserPrincipal adminPrincipal = new UserPrincipal(adminUser);
        PaymentResponse response = paymentService.refundPaymentPartial(
                adminPrincipal, 9001L, BigDecimal.valueOf(300.00), "Remaining balance refund");

        assertNotNull(response);
        assertEquals(PaymentStatus.REFUNDED, testPayment.getStatus());
        assertEquals(BigDecimal.valueOf(500.00), testPayment.getRefundedAmount());

        ArgumentCaptor<PaymentTransaction> txCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).save(txCaptor.capture());
        assertEquals(PaymentEventType.REFUND_COMPLETED, txCaptor.getValue().getEventType());

        // Full refund triggers order cancellation
        verify(orderService).markCancelledDueToRefund(testOrder);
    }

    @Test
    @DisplayName("Workstream 4: Rejects refund when requested amount exceeds remaining refundable amount")
    void testRejectsRefundExceedingMaxRefundable() {
        testPayment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
        testPayment.setRefundedAmount(BigDecimal.valueOf(400.00)); // only 100 remaining

        when(paymentRepository.findById(9001L)).thenReturn(Optional.of(testPayment));

        UserPrincipal adminPrincipal = new UserPrincipal(adminUser);
        assertThrows(IllegalArgumentException.class, () ->
                paymentService.refundPaymentPartial(adminPrincipal, 9001L, BigDecimal.valueOf(150.00), "Excess refund"));

        verify(gatewayClient, never()).refundPayment(anyString(), any());
    }

    @Test
    @DisplayName("Workstream 4: Asynchronous refund.processed webhook is idempotent and prevents duplicate transactions")
    void testRefundProcessedWebhookDeduplication() {
        String webhookPayload = """
                {
                  "event": "refund.processed",
                  "payload": {
                    "refund": {
                      "entity": {
                        "id": "rfnd_test_auto_500",
                        "payment_id": "pay_test_9001",
                        "amount": 50000
                      }
                    }
                  }
                }
                """;

        when(signatureVerifier.verifyWebhookSignature(anyString(), anyString())).thenReturn(true);
        when(paymentRepository.findByRazorpayPaymentId("pay_test_9001")).thenReturn(Optional.of(testPayment));

        // First call: No duplicate exists
        PaymentTransaction existingTx = PaymentTransaction.builder()
                .payment(testPayment)
                .eventType(PaymentEventType.WEBHOOK_REFUND_PROCESSED)
                .razorpayPaymentId("pay_test_9001")
                .rawPayload("Webhook refund processed: rfnd_test_auto_500")
                .build();

        when(paymentTransactionRepository.findByPaymentIdOrderByCreatedAtAsc(9001L))
                .thenReturn(Collections.emptyList()) // 1st invocation
                .thenReturn(List.of(existingTx));     // 2nd invocation

        // Invocation 1: Processes webhook
        paymentService.handleWebhookEvent(webhookPayload, "valid_signature_1");
        verify(paymentRepository, times(1)).save(testPayment);

        // Invocation 2: Duplicate webhook ignored
        paymentService.handleWebhookEvent(webhookPayload, "valid_signature_2");
        verify(paymentRepository, times(1)).save(testPayment); // not called again
    }

    @Test
    @DisplayName("Workstream 4: Asynchronous refund.failed webhook records failure audit log safely")
    void testRefundFailedWebhookHandling() {
        String webhookPayload = """
                {
                  "event": "refund.failed",
                  "payload": {
                    "refund": {
                      "entity": {
                        "id": "rfnd_failed_99",
                        "payment_id": "pay_test_9001"
                      }
                    }
                  }
                }
                """;

        when(signatureVerifier.verifyWebhookSignature(anyString(), anyString())).thenReturn(true);
        when(paymentRepository.findByRazorpayPaymentId("pay_test_9001")).thenReturn(Optional.of(testPayment));

        paymentService.handleWebhookEvent(webhookPayload, "valid_signature");

        ArgumentCaptor<PaymentTransaction> txCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).save(txCaptor.capture());
        assertEquals(PaymentEventType.WEBHOOK_REFUND_FAILED, txCaptor.getValue().getEventType());
        assertTrue(txCaptor.getValue().getRawPayload().contains("rfnd_failed_99"));
        // Payment status is preserved
        assertEquals(PaymentStatus.SUCCESS, testPayment.getStatus());
    }

    @Test
    @DisplayName("Workstream 1 & 4: Verified payment commits inventory reservations")
    void testVerifiedPaymentCommitsInventoryReservation() {
        Payment pendingPayment = Payment.builder()
                .id(9002L)
                .order(testOrder)
                .amount(BigDecimal.valueOf(500.00))
                .status(PaymentStatus.PENDING)
                .razorpayOrderId("order_test_9002")
                .build();

        PaymentResultRequest request = new PaymentResultRequest("pay_test_9002", "valid_signature");

        when(paymentRepository.findById(9002L)).thenReturn(Optional.of(pendingPayment));
        when(signatureVerifier.verifyPaymentSignature("order_test_9002", "pay_test_9002", "valid_signature"))
                .thenReturn(true);

        UserPrincipal customerPrincipal = new UserPrincipal(customer);
        paymentService.recordClientReportedResult(customerPrincipal, 9002L, request);

        // Verification: Reservation commitment must be triggered
        verify(inventoryReservationService).commitReservationsForOrder(testOrder.getId());
        verify(orderService).markPaid(testOrder);
        assertEquals(PaymentStatus.SUCCESS, pendingPayment.getStatus());
    }
}
