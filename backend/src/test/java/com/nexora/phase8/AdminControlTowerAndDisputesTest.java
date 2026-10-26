package com.nexora.phase8;

import com.nexora.admin.dto.*;
import com.nexora.admin.service.AdminControlTowerService;
import com.nexora.auth.security.UserPrincipal;
import com.nexora.category.entity.Category;
import com.nexora.category.repository.CategoryRepository;
import com.nexora.delivery.entity.DeliveryPartner;
import com.nexora.delivery.entity.DeliveryPartnerStatus;
import com.nexora.delivery.repository.DeliveryAssignmentRepository;
import com.nexora.delivery.repository.DeliveryPartnerRepository;
import com.nexora.dispute.dto.CreateDisputeRequest;
import com.nexora.dispute.dto.DisputeDecisionRequest;
import com.nexora.dispute.dto.DisputeResponse;
import com.nexora.dispute.entity.Dispute;
import com.nexora.dispute.entity.DisputeStatus;
import com.nexora.dispute.repository.DisputeRepository;
import com.nexora.inventory.repository.InventoryRepository;
import com.nexora.order.entity.Order;
import com.nexora.order.entity.OrderStatus;
import com.nexora.order.repository.OrderRepository;
import com.nexora.payment.repository.PaymentRepository;
import com.nexora.pricing.dto.SurgePricingDto;
import com.nexora.pricing.entity.SurgePricingRule;
import com.nexora.pricing.repository.SurgePricingRepository;
import com.nexora.product.entity.Product;
import com.nexora.product.repository.ProductRepository;
import com.nexora.role.entity.Role;
import com.nexora.role.entity.RoleName;
import com.nexora.store.entity.Store;
import com.nexora.store.repository.StoreRepository;
import com.nexora.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminControlTowerAndDisputesTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private DeliveryPartnerRepository partnerRepository;
    @Mock
    private DeliveryAssignmentRepository assignmentRepository;
    @Mock
    private DisputeRepository disputeRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private SurgePricingRepository surgePricingRepository;
    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private AdminControlTowerService adminService;

    private User superAdminUser;
    private User customerUser;
    private UserPrincipal superAdminPrincipal;
    private UserPrincipal customerPrincipal;
    private Order testOrder;
    private Store testStore;
    private Dispute testDispute;

    @BeforeEach
    void setUp() {
        Role superAdminRole = Role.builder().id(1L).name(RoleName.SUPER_ADMIN).build();
        Role customerRole = Role.builder().id(2L).name(RoleName.CUSTOMER).build();

        superAdminUser = User.builder().id(1L).name("Super Admin").roles(Set.of(superAdminRole)).build();
        customerUser = User.builder().id(2L).name("Customer Dave").roles(Set.of(customerRole)).build();

        superAdminPrincipal = new UserPrincipal(superAdminUser);
        customerPrincipal = new UserPrincipal(customerUser);

        testStore = Store.builder().id(4L).name("Nexora Dark Store SG Highway").build();

        testOrder = Order.builder()
                .id(7001L)
                .user(customerUser)
                .store(testStore)
                .status(OrderStatus.CONFIRMED)
                .subtotal(BigDecimal.valueOf(250.00))
                .deliveryFee(BigDecimal.valueOf(25.00))
                .totalAmount(BigDecimal.valueOf(275.00))
                .paymentMethod("ONLINE")
                .paymentStatus("PAID")
                .createdAt(LocalDateTime.now().minusMinutes(15)) // 15 mins ago, delayed past 10m SLA
                .build();

        testDispute = Dispute.builder()
                .id(301L)
                .order(testOrder)
                .customer(customerUser)
                .reason("Damaged milk carton delivered")
                .claimAmount(BigDecimal.valueOf(56.00))
                .status(DisputeStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Control Tower Authorization: Regular customer cannot access Super Admin dashboard")
    void getControlTowerOverview_CustomerCaller_ThrowsAccessDeniedException() {
        assertThrows(AccessDeniedException.class, () ->
                adminService.getControlTowerOverview(customerPrincipal));
    }

    @Test
    @DisplayName("Control Tower: Super Admin retrieves GMV, hub health, and delayed order alerts")
    void getControlTowerOverview_SuperAdmin_ReturnsMetricsAndDelayedAlerts() {
        when(orderRepository.findAll()).thenReturn(List.of(testOrder));
        when(storeRepository.findAll()).thenReturn(List.of(testStore));
        when(partnerRepository.findByStatusAndIsOnlineTrue(DeliveryPartnerStatus.AVAILABLE)).thenReturn(List.of());
        when(disputeRepository.findByStatusOrderByCreatedAtDesc(DisputeStatus.OPEN)).thenReturn(List.of(testDispute));

        ControlTowerOverviewResponse overview = adminService.getControlTowerOverview(superAdminPrincipal);

        assertNotNull(overview);
        assertEquals(1, overview.getTotalOrders());
        assertEquals(1, overview.getActiveOrders());
        assertEquals(1, overview.getDelayedOrdersCount());
        assertEquals(BigDecimal.valueOf(275.00), overview.getGrossMerchandiseValue());
        assertEquals(1, overview.getDelayedOrders().size());
        assertEquals("ORD-7001", overview.getDelayedOrders().get(0).getOrderNumber());
        assertTrue(overview.getDelayedOrders().get(0).getElapsedSeconds() >= 600);
    }

    @Test
    @DisplayName("Disputes: Customer logs dispute and refund claim")
    void createDispute_ValidOrder_CreatesOpenDispute() {
        when(orderRepository.findById(7001L)).thenReturn(Optional.of(testOrder));
        when(disputeRepository.existsByOrderIdAndStatusIn(eq(7001L), any())).thenReturn(false);
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(i -> {
            Dispute d = i.getArgument(0);
            d.setId(302L);
            return d;
        });

        CreateDisputeRequest req = CreateDisputeRequest.builder()
                .orderId(7001L)
                .reason("Missing item from bag")
                .customerNotes("Ordered 2 milk packets, received 1")
                .claimAmount(BigDecimal.valueOf(28.00))
                .build();

        DisputeResponse res = adminService.createDispute(customerPrincipal, req);

        assertNotNull(res);
        assertEquals(302L, res.getId());
        assertEquals(DisputeStatus.OPEN, res.getStatus());
        assertEquals(BigDecimal.valueOf(28.00), res.getClaimAmount());
    }

    @Test
    @DisplayName("Disputes: Duplicate active dispute on same order is rejected")
    void createDispute_DuplicateActiveDispute_ThrowsIllegalStateException() {
        when(orderRepository.findById(7001L)).thenReturn(Optional.of(testOrder));
        when(disputeRepository.existsByOrderIdAndStatusIn(eq(7001L), any())).thenReturn(true);

        CreateDisputeRequest req = CreateDisputeRequest.builder()
                .orderId(7001L)
                .reason("Duplicate claim")
                .claimAmount(BigDecimal.valueOf(50.00))
                .build();

        assertThrows(IllegalStateException.class, () ->
                adminService.createDispute(customerPrincipal, req));
    }

    @Test
    @DisplayName("Refunds: Approving dispute cancels order, transitions payment to REFUNDED, generates audit ID")
    void processDisputeDecision_ApproveRefund_CancelsOrderAndSetsRefunded() {
        when(disputeRepository.findById(301L)).thenReturn(Optional.of(testDispute));
        when(paymentRepository.findByOrderId(7001L)).thenReturn(List.of());
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(i -> i.getArgument(0));

        DisputeDecisionRequest decisionReq = DisputeDecisionRequest.builder()
                .decision(DisputeStatus.APPROVED_REFUNDED)
                .decisionNotes("Verified missing SKU with packer camera audit")
                .build();

        DisputeResponse res = adminService.processDisputeDecision(superAdminPrincipal, 301L, decisionReq);

        assertNotNull(res);
        assertEquals(DisputeStatus.APPROVED_REFUNDED, res.getStatus());
        assertNotNull(res.getRefundTransactionId());
        assertTrue(res.getRefundTransactionId().startsWith("REF-"));
        assertEquals(OrderStatus.CANCELLED, testOrder.getStatus());
        assertEquals("REFUNDED", testOrder.getPaymentStatus());
    }

    @Test
    @DisplayName("Refunds Idempotency: Duplicate refund on already approved dispute is blocked")
    void processDisputeDecision_AlreadyRefunded_PreventsDuplicateRefund() {
        testDispute.setStatus(DisputeStatus.APPROVED_REFUNDED);
        when(disputeRepository.findById(301L)).thenReturn(Optional.of(testDispute));

        DisputeDecisionRequest decisionReq = DisputeDecisionRequest.builder()
                .decision(DisputeStatus.APPROVED_REFUNDED)
                .decisionNotes("Repeat attempt")
                .build();

        assertThrows(IllegalStateException.class, () ->
                adminService.processDisputeDecision(superAdminPrincipal, 301L, decisionReq));
    }

    @Test
    @DisplayName("Surge Pricing Safety: Multiplier exceeding 2.50x is rejected to prevent price gouging")
    void updateSurgePricingRule_MultiplierExceedsLimit_ThrowsIllegalArgumentException() {
        SurgePricingDto req = SurgePricingDto.builder()
                .hubId(4L)
                .multiplier(BigDecimal.valueOf(3.50)) // Exceeds 2.50 safety cap
                .extraFlatFee(BigDecimal.ZERO)
                .isActive(true)
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                adminService.updateSurgePricingRule(superAdminPrincipal, req));
    }

    @Test
    @DisplayName("Surge Pricing: Valid multiplier and fee updates rule with audit metadata")
    void updateSurgePricingRule_ValidRule_UpdatesAndLogsAudit() {
        SurgePricingRule existing = SurgePricingRule.builder()
                .hubId(4L)
                .hubName("SG Highway")
                .multiplier(BigDecimal.ONE)
                .extraFlatFee(BigDecimal.ZERO)
                .isActive(false)
                .build();

        when(surgePricingRepository.findByHubId(4L)).thenReturn(Optional.of(existing));
        when(surgePricingRepository.save(any(SurgePricingRule.class))).thenAnswer(i -> i.getArgument(0));

        SurgePricingDto req = SurgePricingDto.builder()
                .hubId(4L)
                .multiplier(BigDecimal.valueOf(1.40))
                .extraFlatFee(BigDecimal.valueOf(15.00))
                .isActive(true)
                .reason("Heavy rainfall alert in western Ahmedabad")
                .build();

        SurgePricingDto res = adminService.updateSurgePricingRule(superAdminPrincipal, req);

        assertNotNull(res);
        assertTrue(res.getIsActive());
        assertEquals(BigDecimal.valueOf(1.40), res.getMultiplier());
        assertEquals(BigDecimal.valueOf(15.00), res.getExtraFlatFee());
        assertEquals("Super Admin", res.getUpdatedBy());
    }

    @Test
    @DisplayName("Price Override: Admin updates product selling price with audit log")
    void overrideProductPrice_Admin_UpdatesPriceAndAudit() {
        Product prod = Product.builder()
                .id(101L)
                .name("Dairy Milk Silk 60g")
                .price(BigDecimal.valueOf(80.00))
                .discount(BigDecimal.ZERO)
                .build();

        when(productRepository.findById(101L)).thenReturn(Optional.of(prod));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        PriceOverrideRequest req = PriceOverrideRequest.builder()
                .overridePrice(BigDecimal.valueOf(69.00))
                .overrideDiscount(BigDecimal.valueOf(11.00))
                .reason("Flash weekend promotion")
                .build();

        Product updated = adminService.overrideProductPrice(superAdminPrincipal, 101L, req);

        assertNotNull(updated);
        assertEquals(BigDecimal.valueOf(69.00), updated.getPrice());
        assertEquals(BigDecimal.valueOf(11.00), updated.getDiscount());
    }

    @Test
    @DisplayName("Bulk Catalog Import: Valid CSV rows staged and committed atomically")
    void bulkImportCatalog_ValidCsv_ImportsProductsAndInventories() {
        when(storeRepository.findById(4L)).thenReturn(Optional.of(testStore));
        when(categoryRepository.findByName("Snacks")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> {
            Product p = i.getArgument(0);
            p.setId(991L);
            return p;
        });

        String csv = "name,category,price,mrp,unit,stock\n" +
                     "Balaji Wafers Cream & Onion 65g,Snacks,20.00,20.00,65g,100\n";

        BulkCatalogImportResult res = adminService.bulkImportCatalog(superAdminPrincipal, csv);

        assertNotNull(res);
        assertEquals(1, res.getSuccessCount());
        assertEquals(0, res.getFailureCount());
        assertEquals(1, res.getImportedProductIds().size());
        verify(inventoryRepository, times(1)).save(any());
    }
}
