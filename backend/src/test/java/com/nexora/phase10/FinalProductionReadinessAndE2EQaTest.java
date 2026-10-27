package com.nexora.phase10;

import com.nexora.admin.dto.analytics.AnalyticsOverviewResponse;
import com.nexora.admin.service.AdminControlTowerService;
import com.nexora.admin.service.BusinessAnalyticsService;
import com.nexora.auth.security.UserPrincipal;
import com.nexora.common.filter.CorrelationIdFilter;
import com.nexora.delivery.entity.DeliveryAssignment;
import com.nexora.delivery.entity.DeliveryPartner;
import com.nexora.delivery.entity.DeliveryPartnerStatus;
import com.nexora.delivery.repository.DeliveryAssignmentRepository;
import com.nexora.delivery.repository.DeliveryPartnerRepository;
import com.nexora.delivery.service.DeliveryService;
import com.nexora.dispute.dto.CreateDisputeRequest;
import com.nexora.dispute.dto.DisputeDecisionRequest;
import com.nexora.dispute.dto.DisputeResponse;
import com.nexora.dispute.entity.Dispute;
import com.nexora.dispute.entity.DisputeStatus;
import com.nexora.dispute.repository.DisputeRepository;
import com.nexora.exception.ForbiddenOperationException;
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
import com.nexora.order.exception.InvalidOrderStatusTransitionException;
import com.nexora.order.repository.OrderItemRepository;
import com.nexora.order.repository.OrderRepository;
import com.nexora.order.service.OrderService;
import com.nexora.payment.entity.Payment;
import com.nexora.payment.entity.PaymentStatus;
import com.nexora.payment.repository.PaymentRepository;
import com.nexora.payment.security.RazorpaySignatureVerifier;
import com.nexora.payment.service.PaymentService;
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
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FinalProductionReadinessAndE2EQaTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private DeliveryPartnerRepository partnerRepository;
    @Mock
    private DeliveryAssignmentRepository assignmentRepository;
    @Mock
    private DeliveryService deliveryService;
    @Mock
    private DisputeRepository disputeRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentService paymentService;
    @Mock
    private NotificationLogRepository notificationLogRepository;

    private User customer;
    private User customerOther;
    private User storeStaff;
    private User rider;
    private User superAdmin;

    private UserPrincipal customerPrincipal;
    private UserPrincipal customerOtherPrincipal;
    private UserPrincipal storeStaffPrincipal;
    private UserPrincipal riderPrincipal;
    private UserPrincipal superAdminPrincipal;

    private Store darkStore;
    private Product sampleProduct;

    @BeforeEach
    void init() {
        customer = User.builder().id(101L).name("Priya Sharma").email("priya@nexora.com").build();
        customer.setRoles(Set.of(Role.builder().id(1L).name(RoleName.CUSTOMER).build()));
        customerPrincipal = new UserPrincipal(customer);

        customerOther = User.builder().id(102L).name("Rahul Verma").email("rahul@nexora.com").build();
        customerOther.setRoles(Set.of(Role.builder().id(1L).name(RoleName.CUSTOMER).build()));
        customerOtherPrincipal = new UserPrincipal(customerOther);

        storeStaff = User.builder().id(201L).name("Store Manager Amit").email("amit@store.nexora.com").build();
        storeStaff.setRoles(Set.of(Role.builder().id(2L).name(RoleName.STORE_MANAGER).build()));
        storeStaffPrincipal = new UserPrincipal(storeStaff);

        rider = User.builder().id(301L).name("Rider Suresh").email("suresh@delivery.nexora.com").build();
        rider.setRoles(Set.of(Role.builder().id(3L).name(RoleName.DELIVERY_PARTNER).build()));
        riderPrincipal = new UserPrincipal(rider);

        superAdmin = User.builder().id(999L).name("Super Admin").email("admin@nexora.com").build();
        superAdmin.setRoles(Set.of(Role.builder().id(4L).name(RoleName.SUPER_ADMIN).build()));
        superAdminPrincipal = new UserPrincipal(superAdmin);

        darkStore = Store.builder().id(1L).name("Nexora Vastrapur Dark Store #02").owner(storeStaff).build();
        sampleProduct = Product.builder().id(10L).name("Amul Gold Milk 500ml").price(new BigDecimal("33.00")).store(darkStore).build();
    }

    private String hashOtp(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(otp.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    @DisplayName("E2E QA 1: Full lifecycle workflow (Confirmed -> Packed -> Delivery OTP Verified -> Delivered)")
    void testCompleteOrderLifecycleAndOtpVerification() {
        String rawOtp = "4821";
        String hashedOtp = hashOtp(rawOtp);

        Order order = Order.builder()
                .id(8001L)
                .user(customer)
                .store(darkStore)
                .status(OrderStatus.CONFIRMED)
                .totalAmount(new BigDecimal("180.00"))
                .deliveryOtpHash(hashedOtp)
                .otpAttempts(0)
                .createdAt(LocalDateTime.now())
                .build();

        OrderItem item = OrderItem.builder()
                .id(901L)
                .order(order)
                .product(sampleProduct)
                .productNameSnapshot("Amul Gold Milk 500ml")
                .unitPrice(new BigDecimal("33.00"))
                .quantity(2)
                .subtotal(new BigDecimal("66.00"))
                .isPicked(false)
                .build();

        order.setItems(Set.of(item));

        // 1. Store Operations: Picking
        StoreOperationsService storeOps = new StoreOperationsService(
                orderRepository, orderItemRepository, storeRepository,
                productRepository, inventoryRepository, deliveryService
        );

        when(orderRepository.findById(8001L)).thenReturn(Optional.of(order));
        when(storeRepository.findById(1L)).thenReturn(Optional.of(darkStore));
        when(orderItemRepository.findById(901L)).thenReturn(Optional.of(item));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        storeOps.startPicking(storeStaffPrincipal, 8001L);
        PickItemRequest pickReq = new PickItemRequest();
        storeOps.pickItem(storeStaffPrincipal, 8001L, 901L, pickReq);
        assertTrue(item.getIsPicked());

        // 2. Store Operations: Packing
        CompletePackingRequest packReq = new CompletePackingRequest();
        packReq.setBagCode("BAG-NX-8821");
        OrderResponse packedRes = storeOps.completePacking(storeStaffPrincipal, 8001L, packReq);
        assertEquals(OrderStatus.READY.name(), packedRes.getStatus());

        // 3. Delivery OTP Verification by Rider
        OrderService orderService = new OrderService(
                orderRepository, orderItemRepository, null, null,
                storeRepository, null, inventoryService, null,
                deliveryService, null
        );

        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        VerifyDeliveryOtpRequest otpReq = new VerifyDeliveryOtpRequest();
        otpReq.setOtp(rawOtp);

        OrderResponse deliveredRes = orderService.verifyDeliveryOtp(riderPrincipal, 8001L, otpReq);
        assertEquals(OrderStatus.DELIVERED.name(), deliveredRes.getStatus());
        verify(orderRepository, atLeast(2)).save(any(Order.class));
    }

    @Test
    @DisplayName("E2E QA 2: Invalid OTP increments attempts and locks after 3 failures")
    void testDeliveryOtpLockoutAfterThreeFailures() {
        String validOtp = "5522";
        Order order = Order.builder()
                .id(8002L)
                .user(customer)
                .store(darkStore)
                .status(OrderStatus.OUT_FOR_DELIVERY)
                .deliveryOtpHash(hashOtp(validOtp))
                .otpAttempts(2) // already failed twice
                .build();

        OrderService orderService = new OrderService(
                orderRepository, orderItemRepository, null, null,
                storeRepository, null, inventoryService, null,
                deliveryService, null
        );

        when(orderRepository.findById(8002L)).thenReturn(Optional.of(order));

        VerifyDeliveryOtpRequest wrongOtpReq = new VerifyDeliveryOtpRequest();
        wrongOtpReq.setOtp("9999"); // wrong

        // Attempt 3 fails
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.verifyDeliveryOtp(riderPrincipal, 8002L, wrongOtpReq);
        });
        assertEquals(3, order.getOtpAttempts());

        // Attempt 4 is locked
        assertThrows(ForbiddenOperationException.class, () -> {
            orderService.verifyDeliveryOtp(riderPrincipal, 8002L, wrongOtpReq);
        });
    }

    @Test
    @DisplayName("E2E QA 3: Cryptographic HMAC-SHA256 signature verification protects against tampering")
    void testRazorpaySignatureVerificationSecurity() throws Exception {
        String keySecret = "secret_key_prod_test_987654321";
        String webhookSecret = "webhook_secret_prod_test_123456";
        RazorpaySignatureVerifier verifier = new RazorpaySignatureVerifier(keySecret, webhookSecret);

        String orderId = "order_Oq7J1k8yW5xZ2A";
        String paymentId = "pay_Pq8K2l9zX6yA3B";

        // Generate genuine signature
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] bytes = mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        String genuineSignature = sb.toString();

        // Verification of authentic signature succeeds
        assertTrue(verifier.verifyPaymentSignature(orderId, paymentId, genuineSignature));

        // Tampered signature is rejected
        String tamperedSignature = genuineSignature.substring(0, genuineSignature.length() - 4) + "abcd";
        assertFalse(verifier.verifyPaymentSignature(orderId, paymentId, tamperedSignature));

        // Missing or null signature is rejected
        assertFalse(verifier.verifyPaymentSignature(orderId, paymentId, null));
        assertFalse(verifier.verifyPaymentSignature(orderId, paymentId, ""));
    }

    @Test
    @DisplayName("E2E QA 4: Security - Object-level isolation prevents Customer B from accessing Customer A's order")
    void testObjectLevelAccessDenialForUnownedOrder() {
        Order orderOfA = Order.builder()
                .id(8003L)
                .user(customer)
                .store(darkStore)
                .status(OrderStatus.CONFIRMED)
                .build();

        OrderService orderService = new OrderService(
                orderRepository, orderItemRepository, null, null,
                storeRepository, null, inventoryService, null,
                deliveryService, null
        );

        when(orderRepository.findById(8003L)).thenReturn(Optional.of(orderOfA));

        // Customer B cannot view order of Customer A
        assertThrows(ForbiddenOperationException.class, () -> {
            orderService.getOrderTracking(customerOtherPrincipal, 8003L);
        });

        // Super Admin can view all orders
        assertDoesNotThrow(() -> {
            orderService.getOrderTracking(superAdminPrincipal, 8003L);
        });
    }

    @Test
    @DisplayName("E2E QA 5: Dispute refund resolution is idempotent and blocks double-refunds")
    void testDisputeRefundIdempotency() {
        Order order = Order.builder()
                .id(8004L)
                .user(customer)
                .store(darkStore)
                .totalAmount(new BigDecimal("250.00"))
                .status(OrderStatus.DELIVERED)
                .build();

        Dispute dispute = Dispute.builder()
                .id(101L)
                .order(order)
                .customer(customer)
                .claimAmount(new BigDecimal("250.00"))
                .status(DisputeStatus.OPEN)
                .build();

        AdminControlTowerService towerService = new AdminControlTowerService(
                orderRepository, storeRepository, partnerRepository, assignmentRepository,
                disputeRepository, productRepository, null, inventoryRepository,
                null, null, paymentRepository
        );

        when(disputeRepository.findById(101L)).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(i -> i.getArgument(0));

        DisputeDecisionRequest approveReq = DisputeDecisionRequest.builder()
                .decision(DisputeStatus.APPROVED_REFUNDED)
                .decisionNotes("Missing items confirmed by store audit")
                .build();

        // First approval executes cleanly
        DisputeResponse res = towerService.processDisputeDecision(superAdminPrincipal, 101L, approveReq);
        assertEquals(DisputeStatus.APPROVED_REFUNDED, res.getStatus());
        assertEquals("REFUNDED", order.getPaymentStatus());

        // Second approval attempt is blocked to prevent double-refund
        assertThrows(IllegalStateException.class, () -> {
            towerService.processDisputeDecision(superAdminPrincipal, 101L, approveReq);
        });
    }

    @Test
    @DisplayName("E2E QA 6: Notification engine deduplicates identical events and scrubs card numbers")
    void testNotificationEngineSecurityAndDeduplication() {
        NotificationProvider mockProvider = mock(NotificationProvider.class);
        lenient().when(mockProvider.getSupportedChannel()).thenReturn(NotificationChannel.EMAIL);
        lenient().when(mockProvider.send(any())).thenReturn(NotificationDeliveryResult.builder()
                .status(NotificationDeliveryStatus.SIMULATED_LOCAL)
                .providerName("SimProvider")
                .build());

        NotificationService notifService = new NotificationService(notificationLogRepository, List.of(mockProvider));

        when(notificationLogRepository.existsByDeduplicationKey("8005_ORDER_CONFIRMED_EMAIL")).thenReturn(false);
        when(notificationLogRepository.save(any(NotificationLog.class))).thenAnswer(i -> i.getArgument(0));

        NotificationMessage msg = NotificationMessage.builder()
                .orderId(8005L)
                .recipient("customer@nexora.com")
                .channel(NotificationChannel.EMAIL)
                .eventType(NotificationEventType.ORDER_CONFIRMED)
                .title("Order Confirmed")
                .body("Order paid via card 4111 2222 3333 4444 and bearer secret_token_xyz")
                .deduplicationKey("8005_ORDER_CONFIRMED_EMAIL")
                .build();

        NotificationLog logEntry = notifService.dispatch(msg);
        assertNotNull(logEntry);
        assertTrue(logEntry.getSanitizedBody().contains("[REDACTED_CARD]"));
        assertTrue(logEntry.getSanitizedBody().contains("[REDACTED_SECRET]"));

        // Duplicate dispatch test
        when(notificationLogRepository.existsByDeduplicationKey("8005_ORDER_CONFIRMED_EMAIL")).thenReturn(true);
        when(notificationLogRepository.findByDeduplicationKey("8005_ORDER_CONFIRMED_EMAIL")).thenReturn(Optional.of(logEntry));

        NotificationLog secondDispatch = notifService.dispatch(msg);
        assertSame(logEntry, secondDispatch);
        verify(mockProvider, times(1)).send(any()); // provider called only once
    }

    @Test
    @DisplayName("E2E QA 7: Business Analytics correctly reconciles GMV, Net Revenue, and SLA rate")
    void testBusinessAnalyticsEngineCalculations() {
        LocalDateTime now = LocalDateTime.now();
        Order o1 = Order.builder()
                .id(1L)
                .store(darkStore)
                .status(OrderStatus.DELIVERED)
                .totalAmount(new BigDecimal("400.00"))
                .deliveryFee(new BigDecimal("25.00"))
                .createdAt(now.minusSeconds(350)) // 350s -> compliant (<600s)
                .deliveredAt(now)
                .build();

        Order o2 = Order.builder()
                .id(2L)
                .store(darkStore)
                .status(OrderStatus.CANCELLED)
                .totalAmount(new BigDecimal("150.00"))
                .deliveryFee(new BigDecimal("25.00"))
                .createdAt(now.minusSeconds(100))
                .build();

        when(orderRepository.findAll()).thenReturn(List.of(o1, o2));
        when(orderRepository.findByStoreIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(o1, o2));
        when(storeRepository.findAll()).thenReturn(List.of(darkStore));

        Dispute d = Dispute.builder()
                .id(1L)
                .order(o2)
                .status(DisputeStatus.APPROVED_REFUNDED)
                .claimAmount(new BigDecimal("150.00"))
                .build();
        when(disputeRepository.findAll()).thenReturn(List.of(d));

        Payment p = Payment.builder().id(1L).amount(new BigDecimal("550.00")).status(PaymentStatus.SUCCESS).build();
        when(paymentRepository.findAll()).thenReturn(List.of(p));

        BusinessAnalyticsService analytics = new BusinessAnalyticsService(
                orderRepository, orderItemRepository, storeRepository,
                productRepository, inventoryRepository, disputeRepository, paymentRepository
        );

        AnalyticsOverviewResponse res = analytics.getOverview(superAdminPrincipal, null, null, null);
        assertNotNull(res);
        assertEquals(2, res.getTotalOrders());
        assertEquals(new BigDecimal("550.00"), res.getGrossMerchandiseValue());
        assertEquals(1, res.getTotalCancellations());
        assertEquals(100.0, res.getSlaComplianceRatePercent()); // 1 delivered order was <600s
        assertEquals(new BigDecimal("150.00"), res.getTotalRefundsAmount());
    }

    @Test
    @DisplayName("E2E QA 8: Observability - CorrelationIdFilter propagates trace ID and cleans MDC")
    void testCorrelationIdMdcFilter() throws Exception {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn("qa-trace-uuid-99");

        filter.doFilter(request, response, chain);

        verify(response).setHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "qa-trace-uuid-99");
        verify(chain).doFilter(request, response);
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }
}
