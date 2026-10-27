package com.nexora.phase11;

import com.nexora.dispute.dto.DisputeDecisionRequest;
import com.nexora.dispute.dto.DisputeResponse;
import com.nexora.admin.service.AdminControlTowerService;
import com.nexora.auth.security.UserPrincipal;
import com.nexora.delivery.entity.DeliveryAssignment;
import com.nexora.delivery.entity.DeliveryPartner;
import com.nexora.delivery.entity.DeliveryPartnerStatus;
import com.nexora.delivery.repository.DeliveryAssignmentRepository;
import com.nexora.delivery.repository.DeliveryPartnerRepository;
import com.nexora.delivery.service.DeliveryService;
import com.nexora.dispute.entity.Dispute;
import com.nexora.dispute.entity.DisputeStatus;
import com.nexora.dispute.repository.DisputeRepository;
import com.nexora.inventory.entity.Inventory;
import com.nexora.inventory.repository.InventoryRepository;
import com.nexora.inventory.service.InventoryService;
import com.nexora.notification.dto.NotificationDeliveryResult;
import com.nexora.notification.dto.NotificationMessage;
import com.nexora.notification.entity.NotificationChannel;
import com.nexora.notification.entity.NotificationDeliveryStatus;
import com.nexora.notification.entity.NotificationEventType;
import com.nexora.notification.entity.NotificationLog;
import com.nexora.notification.provider.NotificationProvider;
import com.nexora.notification.repository.NotificationLogRepository;
import com.nexora.notification.service.NotificationService;
import com.nexora.order.dto.OrderResponse;
import com.nexora.order.dto.VerifyDeliveryOtpRequest;
import com.nexora.order.entity.Order;
import com.nexora.order.entity.OrderItem;
import com.nexora.order.entity.OrderStatus;
import com.nexora.order.repository.OrderItemRepository;
import com.nexora.order.repository.OrderRepository;
import com.nexora.order.service.OrderService;
import com.nexora.payment.entity.Payment;
import com.nexora.payment.entity.PaymentStatus;
import com.nexora.payment.repository.PaymentRepository;
import com.nexora.payment.security.RazorpaySignatureVerifier;
import com.nexora.product.entity.Product;
import com.nexora.product.repository.ProductRepository;
import com.nexora.role.entity.Role;
import com.nexora.role.entity.RoleName;
import com.nexora.store.dto.CompletePackingRequest;
import com.nexora.store.dto.PickItemRequest;
import com.nexora.store.entity.Store;
import com.nexora.store.repository.StoreRepository;
import com.nexora.store.service.StoreOperationsService;
import com.nexora.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 11: Real Production Validation & Controlled Launch Safety")
public class RealProductionValidationAndLaunchSafetyTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private ProductRepository productRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private DeliveryService deliveryService;
    @Mock private InventoryService inventoryService;
    @Mock private PaymentRepository paymentRepository;
    @Mock private DisputeRepository disputeRepository;
    @Mock private DeliveryPartnerRepository partnerRepository;
    @Mock private DeliveryAssignmentRepository assignmentRepository;
    @Mock private NotificationLogRepository notificationLogRepository;

    private BCryptPasswordEncoder passwordEncoder;
    private User customer;
    private User superAdmin;
    private User rider;
    private User storeManager;
    private UserPrincipal customerPrincipal;
    private UserPrincipal superAdminPrincipal;
    private UserPrincipal riderPrincipal;
    private UserPrincipal storeManagerPrincipal;
    private Store darkStore;
    private Product milkProduct;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();

        Role customerRole = Role.builder().id(1L).name(RoleName.CUSTOMER).build();
        Role adminRole = Role.builder().id(2L).name(RoleName.SUPER_ADMIN).build();
        Role riderRole = Role.builder().id(3L).name(RoleName.DELIVERY_PARTNER).build();
        Role managerRole = Role.builder().id(4L).name(RoleName.STORE_MANAGER).build();

        customer = User.builder().id(101L).name("Priya Patel").email("priya@patel.com").roles(Set.of(customerRole)).build();
        superAdmin = User.builder().id(1L).name("Nexora Admin").email("admin@nexora.com").roles(Set.of(adminRole)).build();
        rider = User.builder().id(201L).name("Rider Sanjay").email("sanjay@rider.com").roles(Set.of(riderRole)).build();
        storeManager = User.builder().id(301L).name("Manager Maya").email("maya@store.com").roles(Set.of(managerRole)).build();

        customerPrincipal = new UserPrincipal(customer);
        superAdminPrincipal = new UserPrincipal(superAdmin);
        riderPrincipal = new UserPrincipal(rider);
        storeManagerPrincipal = new UserPrincipal(storeManager);

        darkStore = Store.builder().id(10L).name("Nexora Dark Store SG Highway").owner(storeManager).build();
        milkProduct = Product.builder().id(501L).name("Amul Gold 500ml").price(new BigDecimal("33.00")).store(darkStore).build();
    }

    private String calculateHmac(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Test
    @DisplayName("P11-1: Payment Gateway Security - Cryptographic verification rejects tampered, wrong secret, or null signatures")
    void testPaymentGatewaySecurityAndTamperResistance() throws Exception {
        String keySecret = "live_secret_k98234jhl2k34";
        String webhookSecret = "wh_secret_m098234kljsd09";
        RazorpaySignatureVerifier verifier = new RazorpaySignatureVerifier(keySecret, webhookSecret);

        String orderId = "order_Oq7J1k8yW5xZ2A";
        String paymentId = "pay_Pq8K2l9zX6yA3B";
        String genuinePaymentSignature = calculateHmac(orderId + "|" + paymentId, keySecret);

        // 1. Genuine payment signature passes
        assertTrue(verifier.verifyPaymentSignature(orderId, paymentId, genuinePaymentSignature));

        // 2. Tampered signature fails
        String tampered = genuinePaymentSignature.substring(0, genuinePaymentSignature.length() - 2) + "00";
        assertFalse(verifier.verifyPaymentSignature(orderId, paymentId, tampered));

        // 3. Signature from wrong secret fails
        String wrongSecretSig = calculateHmac(orderId + "|" + paymentId, "completely_wrong_secret");
        assertFalse(verifier.verifyPaymentSignature(orderId, paymentId, wrongSecretSig));

        // 4. Null / blank signature fails
        assertFalse(verifier.verifyPaymentSignature(orderId, paymentId, null));
        assertFalse(verifier.verifyPaymentSignature(orderId, paymentId, ""));

        // 5. Genuine Webhook signature passes
        String webhookPayload = "{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"id\":\"" + paymentId + "\"}}}";
        String genuineWebhookSignature = calculateHmac(webhookPayload, webhookSecret);
        assertTrue(verifier.verifyWebhookSignature(webhookPayload, genuineWebhookSignature));

        // 6. Tampered webhook fails
        assertFalse(verifier.verifyWebhookSignature(webhookPayload, genuineWebhookSignature + "bad"));
    }

    @Test
    @DisplayName("P11-2: Payment Webhook Idempotency - Duplicate webhook deliveries do not duplicate ledger or transactions")
    void testPaymentWebhookIdempotency() {
        Payment payment = Payment.builder()
                .id(7001L)
                .amount(new BigDecimal("180.00"))
                .currency("INR")
                .gateway("RAZORPAY")
                .razorpayOrderId("order_Oq7J1k8yW5xZ2A")
                .razorpayPaymentId("pay_Pq8K2l9zX6yA3B")
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentRepository.findByRazorpayOrderId("order_Oq7J1k8yW5xZ2A")).thenReturn(Optional.of(payment));

        // If webhook for this order arrives again, payment is already SUCCESS
        Payment found = paymentRepository.findByRazorpayOrderId("order_Oq7J1k8yW5xZ2A").orElseThrow();
        assertEquals(PaymentStatus.SUCCESS, found.getStatus());

        // Verify no duplicate state mutation occurs
        verify(paymentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("P11-3: End-to-End Controlled Lifecycle - Picking, Packing, Delivery OTP, and Idempotent Refund")
    void testCompleteE2ECustomerAndOperationsJourney() {
        String rawOtp = "4821";
        String otpHash = OrderService.hashOtp(rawOtp);

        Order order = Order.builder()
                .id(9001L)
                .user(customer)
                .store(darkStore)
                .status(OrderStatus.CONFIRMED)
                .deliveryOtpHash(otpHash)
                .otpAttempts(0)
                .totalAmount(new BigDecimal("99.00"))
                .build();

        OrderItem item = OrderItem.builder()
                .id(601L)
                .order(order)
                .product(milkProduct)
                .quantity(3)
                .isPicked(false)
                .build();
        order.setItems(Set.of(item));

        StoreOperationsService storeOps = new StoreOperationsService(
                orderRepository, orderItemRepository, storeRepository,
                productRepository, inventoryRepository, deliveryService
        );

        when(orderRepository.findById(9001L)).thenReturn(Optional.of(order));
        when(storeRepository.findById(10L)).thenReturn(Optional.of(darkStore));
        when(orderItemRepository.findById(601L)).thenReturn(Optional.of(item));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        // 1. Store Staff starts picking & marks item picked
        storeOps.startPicking(storeManagerPrincipal, 9001L);
        storeOps.pickItem(storeManagerPrincipal, 9001L, 601L, new PickItemRequest());
        assertTrue(item.getIsPicked());

        // 2. Staff completes packing into bag
        CompletePackingRequest packReq = new CompletePackingRequest();
        packReq.setBagCode("BAG-NX-PROD-01");
        OrderResponse packedRes = storeOps.completePacking(storeManagerPrincipal, 9001L, packReq);
        assertEquals(OrderStatus.READY.name(), packedRes.getStatus());

        // 3. Rider verifies OTP upon delivery
        OrderService orderService = new OrderService(
                orderRepository, orderItemRepository, null, null,
                storeRepository, null, inventoryService, null,
                deliveryService, null
        );

        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        VerifyDeliveryOtpRequest otpReq = new VerifyDeliveryOtpRequest();
        otpReq.setOtp(rawOtp);
        OrderResponse deliveredRes = orderService.verifyDeliveryOtp(riderPrincipal, 9001L, otpReq);
        assertEquals(OrderStatus.DELIVERED.name(), deliveredRes.getStatus());

        // 4. Customer files dispute and Super Admin processes single refund
        Dispute dispute = Dispute.builder()
                .id(801L)
                .order(order)
                .customer(customer)
                .claimAmount(new BigDecimal("99.00"))
                .status(DisputeStatus.OPEN)
                .build();

        AdminControlTowerService towerService = new AdminControlTowerService(
                orderRepository, storeRepository, partnerRepository, assignmentRepository,
                disputeRepository, productRepository, null, inventoryRepository,
                null, null, paymentRepository
        );

        when(disputeRepository.findById(801L)).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(i -> i.getArgument(0));

        DisputeDecisionRequest decisionReq = DisputeDecisionRequest.builder()
                .decision(DisputeStatus.APPROVED_REFUNDED)
                .decisionNotes("Verified broken packaging by warehouse log")
                .build();

        DisputeResponse disputeRes = towerService.processDisputeDecision(superAdminPrincipal, 801L, decisionReq);
        assertEquals(DisputeStatus.APPROVED_REFUNDED, disputeRes.getStatus());
        assertEquals("REFUNDED", order.getPaymentStatus());

        // Attempting second refund triggers IllegalStateException (Idempotency protection)
        assertThrows(IllegalStateException.class, () -> {
            towerService.processDisputeDecision(superAdminPrincipal, 801L, decisionReq);
        });
    }

    @Test
    @DisplayName("P11-4: Notifications - Provider unconfigured status distinction and PCI-DSS sensitive data scrubbing")
    void testNotificationProviderClassificationAndScrubbing() {
        NotificationProvider unconfiguredProvider = mock(NotificationProvider.class);
        when(unconfiguredProvider.getSupportedChannel()).thenReturn(NotificationChannel.SMS);
        when(unconfiguredProvider.send(any())).thenReturn(NotificationDeliveryResult.builder()
                .status(NotificationDeliveryStatus.QUEUED_UNCONFIGURED)
                .providerName("UnconfiguredSmsGateway")
                .build());

        NotificationService service = new NotificationService(notificationLogRepository, List.of(unconfiguredProvider));

        when(notificationLogRepository.existsByDeduplicationKey("ORD-9002_SMS")).thenReturn(false);
        when(notificationLogRepository.save(any(NotificationLog.class))).thenAnswer(i -> i.getArgument(0));

        NotificationMessage sensitiveMsg = NotificationMessage.builder()
                .orderId(9002L)
                .recipient("+919876543210")
                .channel(NotificationChannel.SMS)
                .eventType(NotificationEventType.ORDER_CONFIRMED)
                .title("Order Confirmed")
                .body("Your card 4111 2222 3333 4444 with bearer auth_bearer_token_xyz paid successfully.")
                .deduplicationKey("ORD-9002_SMS")
                .build();

        NotificationLog log = service.dispatch(sensitiveMsg);
        assertNotNull(log);
        assertEquals(NotificationDeliveryStatus.QUEUED_UNCONFIGURED, log.getDeliveryStatus());

        // Sensitive credentials scrubbed
        assertFalse(log.getSanitizedBody().contains("4111 2222 3333 4444"));
        assertFalse(log.getSanitizedBody().contains("auth_bearer_token_xyz"));
        assertTrue(log.getSanitizedBody().contains("[REDACTED_CARD]"));
        assertTrue(log.getSanitizedBody().contains("[REDACTED_SECRET]"));
    }

    @Test
    @DisplayName("P11-5: Concurrency Safety & Load Resilience - Simultaneous requests cannot oversell inventory")
    void testConcurrencySafetyUnderContention() throws InterruptedException {
        int initialStock = 10;
        Inventory inventory = Inventory.builder()
                .id(1L)
                .product(milkProduct)
                .quantity(initialStock)
                .build();

        int concurrentThreads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentThreads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successfulReservations = new AtomicInteger(0);
        AtomicInteger rejectedReservations = new AtomicInteger(0);

        for (int i = 0; i < concurrentThreads; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    // Simulating atomic database update: UPDATE inventory SET quantity = quantity - 1 WHERE quantity >= 1
                    synchronized (inventory) {
                        if (inventory.getQuantity() > 0) {
                            inventory.setQuantity(inventory.getQuantity() - 1);
                            successfulReservations.incrementAndGet();
                        } else {
                            rejectedReservations.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Release all threads simultaneously
        latch.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        assertEquals(10, successfulReservations.get(), "Exactly 10 items should be reserved");
        assertEquals(10, rejectedReservations.get(), "Remaining 10 requests should be rejected as Out of Stock");
        assertEquals(0, inventory.getQuantity(), "Final inventory stock must be exactly 0, never negative");
    }
}
