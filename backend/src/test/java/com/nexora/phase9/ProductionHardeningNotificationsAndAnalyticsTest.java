package com.nexora.phase9;

import com.nexora.admin.dto.analytics.AnalyticsOverviewResponse;
import com.nexora.admin.service.AdminControlTowerService;
import com.nexora.admin.service.BusinessAnalyticsService;
import com.nexora.auth.security.UserPrincipal;
import com.nexora.common.filter.CorrelationIdFilter;
import com.nexora.dispute.dto.CreateDisputeRequest;
import com.nexora.dispute.entity.Dispute;
import com.nexora.dispute.entity.DisputeStatus;
import com.nexora.dispute.repository.DisputeRepository;
import com.nexora.exception.ForbiddenOperationException;
import com.nexora.inventory.repository.InventoryRepository;
import com.nexora.notification.dto.NotificationDeliveryResult;
import com.nexora.notification.dto.NotificationMessage;
import com.nexora.notification.entity.NotificationChannel;
import com.nexora.notification.entity.NotificationDeliveryStatus;
import com.nexora.notification.entity.NotificationEventType;
import com.nexora.notification.entity.NotificationLog;
import com.nexora.notification.provider.EmailNotificationProvider;
import com.nexora.notification.provider.NotificationProvider;
import com.nexora.notification.repository.NotificationLogRepository;
import com.nexora.notification.service.NotificationService;
import com.nexora.order.entity.Order;
import com.nexora.order.entity.OrderItem;
import com.nexora.order.entity.OrderStatus;
import com.nexora.order.repository.OrderItemRepository;
import com.nexora.order.repository.OrderRepository;
import com.nexora.payment.entity.Payment;
import com.nexora.payment.entity.PaymentStatus;
import com.nexora.payment.repository.PaymentRepository;
import com.nexora.product.entity.Product;
import com.nexora.product.repository.ProductRepository;
import com.nexora.role.entity.Role;
import com.nexora.role.entity.RoleName;
import com.nexora.store.entity.Store;
import com.nexora.store.repository.StoreRepository;
import com.nexora.user.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductionHardeningNotificationsAndAnalyticsTest {

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
    private DisputeRepository disputeRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private NotificationLogRepository notificationLogRepository;

    private BusinessAnalyticsService analyticsService;
    private NotificationService notificationService;

    private User customerA;
    private User customerB;
    private User superAdminUser;
    private UserPrincipal customerAPrincipal;
    private UserPrincipal customerBPrincipal;
    private UserPrincipal superAdminPrincipal;
    private Store store;

    @BeforeEach
    void setUp() {
        analyticsService = new BusinessAnalyticsService(
                orderRepository,
                orderItemRepository,
                storeRepository,
                productRepository,
                inventoryRepository,
                disputeRepository,
                paymentRepository
        );

        customerA = User.builder().id(101L).name("Customer A").email("a@nexora.com").build();
        customerA.setRoles(Set.of(Role.builder().id(1L).name(RoleName.CUSTOMER).build()));
        customerAPrincipal = new UserPrincipal(customerA);

        customerB = User.builder().id(102L).name("Customer B").email("b@nexora.com").build();
        customerB.setRoles(Set.of(Role.builder().id(1L).name(RoleName.CUSTOMER).build()));
        customerBPrincipal = new UserPrincipal(customerB);

        superAdminUser = User.builder().id(999L).name("Super Admin").email("admin@nexora.com").build();
        superAdminUser.setRoles(Set.of(Role.builder().id(2L).name(RoleName.SUPER_ADMIN).build()));
        superAdminPrincipal = new UserPrincipal(superAdminUser);

        store = Store.builder().id(1L).name("Nexora Central Hub").build();
    }

    @Test
    @DisplayName("Security: Non-admin customer cannot access analytics overview")
    void testNonAdminDeniedAnalyticsAccess() {
        assertThrows(AccessDeniedException.class, () -> {
            analyticsService.assertAdminAccess(customerAPrincipal);
        });
    }

    @Test
    @DisplayName("Security: Super Admin is authorized to access analytics overview")
    void testSuperAdminAllowedAnalyticsAccess() {
        assertDoesNotThrow(() -> {
            analyticsService.assertAdminAccess(superAdminPrincipal);
        });
    }

    @Test
    @DisplayName("Security: Object-level check prevents customer B from disputing customer A's order")
    void testCrossCustomerDisputeDenied() {
        AdminControlTowerService adminTowerService = new AdminControlTowerService(
                orderRepository, storeRepository, null, null,
                disputeRepository, null, null, null, null, null, paymentRepository
        );

        Order orderOfA = Order.builder()
                .id(501L)
                .user(customerA)
                .store(store)
                .totalAmount(new BigDecimal("350.00"))
                .status(OrderStatus.DELIVERED)
                .build();

        when(orderRepository.findById(501L)).thenReturn(Optional.of(orderOfA));

        CreateDisputeRequest req = CreateDisputeRequest.builder()
                .orderId(501L)
                .reason("MISSING_ITEMS")
                .customerNotes("Malicious attempt by customer B")
                .claimAmount(new BigDecimal("100.00"))
                .build();

        assertThrows(ForbiddenOperationException.class, () -> {
            adminTowerService.createDispute(customerBPrincipal, req);
        });

        verify(disputeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Security: Owning customer is authorized to create dispute for their own order")
    void testOwningCustomerCanDispute() {
        AdminControlTowerService adminTowerService = new AdminControlTowerService(
                orderRepository, storeRepository, null, null,
                disputeRepository, null, null, null, null, null, paymentRepository
        );

        Order orderOfA = Order.builder()
                .id(502L)
                .user(customerA)
                .store(store)
                .totalAmount(new BigDecimal("420.00"))
                .status(OrderStatus.DELIVERED)
                .build();

        when(orderRepository.findById(502L)).thenReturn(Optional.of(orderOfA));
        when(disputeRepository.existsByOrderIdAndStatusIn(eq(502L), any())).thenReturn(false);
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(i -> {
            Dispute d = i.getArgument(0);
            d.setId(88L);
            return d;
        });

        CreateDisputeRequest req = CreateDisputeRequest.builder()
                .orderId(502L)
                .reason("DAMAGED_PACKAGE")
                .customerNotes("Milk carton was leaked")
                .claimAmount(new BigDecimal("65.00"))
                .build();

        var res = adminTowerService.createDispute(customerAPrincipal, req);
        assertNotNull(res);
        assertEquals(502L, res.getOrderId());
        assertEquals(DisputeStatus.OPEN, res.getStatus());
        verify(disputeRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Notification: Deduplication key prevents duplicate delivery attempts")
    void testNotificationDeduplication() {
        NotificationProvider mockProvider = mock(NotificationProvider.class);
        lenient().when(mockProvider.getSupportedChannel()).thenReturn(NotificationChannel.EMAIL);

        notificationService = new NotificationService(notificationLogRepository, List.of(mockProvider));

        NotificationLog existing = NotificationLog.builder()
                .id(1L)
                .deduplicationKey("501_ORDER_CONFIRMED_EMAIL")
                .deliveryStatus(NotificationDeliveryStatus.SIMULATED_LOCAL)
                .build();

        when(notificationLogRepository.existsByDeduplicationKey("501_ORDER_CONFIRMED_EMAIL")).thenReturn(true);
        when(notificationLogRepository.findByDeduplicationKey("501_ORDER_CONFIRMED_EMAIL")).thenReturn(Optional.of(existing));

        NotificationMessage msg = NotificationMessage.builder()
                .orderId(501L)
                .recipient("test@nexora.com")
                .channel(NotificationChannel.EMAIL)
                .eventType(NotificationEventType.ORDER_CONFIRMED)
                .title("Order Confirmed")
                .body("Your order #501 is confirmed")
                .deduplicationKey("501_ORDER_CONFIRMED_EMAIL")
                .build();

        NotificationLog result = notificationService.dispatch(msg);
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(mockProvider, never()).send(any());
    }

    @Test
    @DisplayName("Notification: Sensitive credit cards and passwords are redacted before persistence")
    void testNotificationBodySanitization() {
        NotificationProvider mockProvider = mock(NotificationProvider.class);
        when(mockProvider.getSupportedChannel()).thenReturn(NotificationChannel.EMAIL);
        when(mockProvider.send(any())).thenReturn(NotificationDeliveryResult.builder()
                .status(NotificationDeliveryStatus.SIMULATED_LOCAL)
                .providerName("MockProvider")
                .build());

        notificationService = new NotificationService(notificationLogRepository, List.of(mockProvider));
        when(notificationLogRepository.existsByDeduplicationKey(any())).thenReturn(false);
        when(notificationLogRepository.save(any(NotificationLog.class))).thenAnswer(i -> i.getArgument(0));

        NotificationMessage msg = NotificationMessage.builder()
                .orderId(503L)
                .recipient("user@nexora.com")
                .channel(NotificationChannel.EMAIL)
                .eventType(NotificationEventType.PAYMENT_SUCCESS)
                .title("Receipt")
                .body("Processed payment with card 4111 2222 3333 4444 and password=SecretPass123")
                .build();

        NotificationLog saved = notificationService.dispatch(msg);
        assertNotNull(saved);
        assertFalse(saved.getSanitizedBody().contains("4111 2222 3333 4444"));
        assertFalse(saved.getSanitizedBody().contains("SecretPass123"));
        assertTrue(saved.getSanitizedBody().contains("[REDACTED_CARD]"));
        assertTrue(saved.getSanitizedBody().contains("[REDACTED_SECRET]"));
    }

    @Test
    @DisplayName("Notification: Unconfigured provider reports QUEUED_UNCONFIGURED without false delivery claim")
    void testUnconfiguredProviderSafeStatus() {
        EmailNotificationProvider provider = new EmailNotificationProvider("", "noreply@nexora.com", false);
        assertFalse(provider.isConfigured());

        NotificationMessage msg = NotificationMessage.builder()
                .recipient("client@nexora.com")
                .channel(NotificationChannel.EMAIL)
                .title("Welcome")
                .body("Hello!")
                .build();

        NotificationDeliveryResult result = provider.send(msg);
        assertEquals(NotificationDeliveryStatus.QUEUED_UNCONFIGURED, result.getStatus());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("Analytics: Correctly calculates GMV, Net Revenue, SLA rate, and Payment Reconciliation")
    void testAnalyticsCalculations() {
        LocalDateTime now = LocalDateTime.now();
        Order o1 = Order.builder()
                .id(1L)
                .store(store)
                .status(OrderStatus.DELIVERED)
                .totalAmount(new BigDecimal("300.00"))
                .deliveryFee(new BigDecimal("25.00"))
                .createdAt(now.minusSeconds(400)) // 400s -> compliant (<600s)
                .deliveredAt(now)
                .build();

        Order o2 = Order.builder()
                .id(2L)
                .store(store)
                .status(OrderStatus.DELIVERED)
                .totalAmount(new BigDecimal("500.00"))
                .deliveryFee(new BigDecimal("25.00"))
                .createdAt(now.minusSeconds(800)) // 800s -> SLA breach (>600s)
                .deliveredAt(now)
                .build();

        Order o3 = Order.builder()
                .id(3L)
                .store(store)
                .status(OrderStatus.CANCELLED)
                .totalAmount(new BigDecimal("200.00"))
                .deliveryFee(new BigDecimal("25.00"))
                .createdAt(now.minusSeconds(50))
                .build();

        when(orderRepository.findAll()).thenReturn(List.of(o1, o2, o3));
        when(orderRepository.findByStoreIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(o1, o2, o3));

        Dispute d = Dispute.builder()
                .id(10L)
                .order(o3)
                .status(DisputeStatus.APPROVED_REFUNDED)
                .claimAmount(new BigDecimal("200.00"))
                .build();
        when(disputeRepository.findAll()).thenReturn(List.of(d));

        Payment p1 = Payment.builder().id(1L).amount(new BigDecimal("800.00")).status(PaymentStatus.SUCCESS).build();
        when(paymentRepository.findAll()).thenReturn(List.of(p1));
        when(storeRepository.findAll()).thenReturn(List.of(store));

        AnalyticsOverviewResponse res = analyticsService.getOverview(superAdminPrincipal, null, null, null);

        assertNotNull(res);
        assertEquals(3, res.getTotalOrders());
        assertEquals(new BigDecimal("1000.00"), res.getGrossMerchandiseValue()); // 300 + 500 + 200
        assertEquals(1, res.getTotalCancellations());
        assertEquals(new BigDecimal("200.00"), res.getTotalRefundsAmount());
        assertEquals(50.0, res.getSlaComplianceRatePercent()); // 1 of 2 delivered orders met 600s SLA
        assertEquals(new BigDecimal("800.00"), res.getPaymentReconciliation().get("gatewayCollected"));
        assertEquals(new BigDecimal("200.00"), res.getPaymentReconciliation().get("refundsDisbursed"));
        assertEquals(new BigDecimal("600.00"), res.getPaymentReconciliation().get("netSettled"));
    }

    @Test
    @DisplayName("Analytics: Empty dataset safely yields 0.00 GMV and 100% SLA without divide-by-zero")
    void testAnalyticsEmptyDatasetSafe() {
        when(orderRepository.findAll()).thenReturn(Collections.emptyList());
        when(disputeRepository.findAll()).thenReturn(Collections.emptyList());
        when(paymentRepository.findAll()).thenReturn(Collections.emptyList());
        when(storeRepository.findAll()).thenReturn(Collections.emptyList());

        AnalyticsOverviewResponse res = analyticsService.getOverview(superAdminPrincipal, null, null, null);

        assertNotNull(res);
        assertEquals(0, res.getTotalOrders());
        assertEquals(BigDecimal.ZERO, res.getGrossMerchandiseValue());
        assertEquals(BigDecimal.ZERO, res.getAverageOrderValue());
        assertEquals(100.0, res.getSlaComplianceRatePercent());
        assertEquals(0, res.getAverageDeliveryTimeSeconds());
    }

    @Test
    @DisplayName("Observability: CorrelationIdFilter assigns header and clears MDC")
    void testCorrelationIdFilter() throws Exception {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn("custom-trace-12345");

        filter.doFilter(request, response, chain);

        verify(response).setHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "custom-trace-12345");
        verify(chain).doFilter(request, response);
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY), "MDC must be cleared after request");
    }
}
