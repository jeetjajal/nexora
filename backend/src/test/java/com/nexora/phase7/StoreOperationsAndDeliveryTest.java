package com.nexora.phase7;

import com.nexora.address.entity.Address;
import com.nexora.auth.security.UserPrincipal;
import com.nexora.delivery.dto.DeliveryAssignmentDetailResponse;
import com.nexora.delivery.entity.AssignmentStatus;
import com.nexora.delivery.entity.DeliveryAssignment;
import com.nexora.delivery.entity.DeliveryPartner;
import com.nexora.delivery.entity.DeliveryPartnerStatus;
import com.nexora.delivery.repository.DeliveryAssignmentRepository;
import com.nexora.delivery.repository.DeliveryPartnerRepository;
import com.nexora.delivery.service.DeliveryService;
import com.nexora.inventory.entity.Inventory;
import com.nexora.inventory.repository.InventoryRepository;
import com.nexora.order.dto.OrderItemResponse;
import com.nexora.order.dto.OrderResponse;
import com.nexora.order.entity.Order;
import com.nexora.order.entity.OrderItem;
import com.nexora.order.entity.OrderStatus;
import com.nexora.order.repository.OrderItemRepository;
import com.nexora.order.repository.OrderRepository;
import com.nexora.product.entity.Product;
import com.nexora.product.repository.ProductRepository;
import com.nexora.role.entity.Role;
import com.nexora.role.entity.RoleName;
import com.nexora.store.dto.CompletePackingRequest;
import com.nexora.store.dto.PickItemRequest;
import com.nexora.store.dto.StoreQueueMetricsResponse;
import com.nexora.store.dto.StoreStockAdjustmentRequest;
import com.nexora.store.entity.Store;
import com.nexora.store.repository.StoreRepository;
import com.nexora.store.service.StoreOperationsService;
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
public class StoreOperationsAndDeliveryTest {

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
    private DeliveryService deliveryService;
    @Mock
    private DeliveryPartnerRepository partnerRepository;
    @Mock
    private DeliveryAssignmentRepository assignmentRepository;

    @InjectMocks
    private StoreOperationsService storeOpsService;

    private User customerUser;
    private User storeManagerUser;
    private User pickerUser;
    private User packerUser;
    private User riderUser;
    private UserPrincipal customerPrincipal;
    private UserPrincipal storeManagerPrincipal;
    private UserPrincipal pickerPrincipal;
    private UserPrincipal packerPrincipal;
    private UserPrincipal riderPrincipal;

    private Store darkStore;
    private Product testProduct;
    private Order testOrder;
    private OrderItem testItem;
    private DeliveryPartner testPartner;
    private DeliveryAssignment testAssignment;

    @BeforeEach
    void setUp() {
        Role customerRole = Role.builder().id(1L).name(RoleName.CUSTOMER).build();
        Role managerRole = Role.builder().id(2L).name(RoleName.STORE_MANAGER).build();
        Role pickerRole = Role.builder().id(3L).name(RoleName.PICKER).build();
        Role packerRole = Role.builder().id(4L).name(RoleName.PACKER).build();
        Role riderRole = Role.builder().id(5L).name(RoleName.DELIVERY_PARTNER).build();

        customerUser = User.builder().id(101L).name("Customer Dave").email("dave@test.com").roles(Set.of(customerRole)).build();
        storeManagerUser = User.builder().id(102L).name("Manager Maya").email("maya@nexora.com").roles(Set.of(managerRole)).build();
        pickerUser = User.builder().id(103L).name("Picker Prem").email("prem@nexora.com").roles(Set.of(pickerRole)).build();
        packerUser = User.builder().id(104L).name("Packer Pooja").email("pooja@nexora.com").roles(Set.of(packerRole)).build();
        riderUser = User.builder().id(105L).name("Rider Ramesh").email("ramesh@nexora.com").roles(Set.of(riderRole)).build();

        customerPrincipal = new UserPrincipal(customerUser);
        storeManagerPrincipal = new UserPrincipal(storeManagerUser);
        pickerPrincipal = new UserPrincipal(pickerUser);
        packerPrincipal = new UserPrincipal(packerUser);
        riderPrincipal = new UserPrincipal(riderUser);

        darkStore = Store.builder().id(4L).name("Nexora Dark Store SG Highway").owner(storeManagerUser).build();
        testProduct = Product.builder().id(10L).name("Amul Gold Milk 500ml").price(BigDecimal.valueOf(33.00)).store(darkStore).build();

        Address address = Address.builder().id(50L).addressLine1("B-404 Shapath V").city("Ahmedabad").build();

        testOrder = Order.builder()
                .id(5001L)
                .store(darkStore)
                .user(customerUser)
                .deliveryAddress(address)
                .status(OrderStatus.CONFIRMED)
                .subtotal(BigDecimal.valueOf(99.00))
                .deliveryFee(BigDecimal.valueOf(25.00))
                .totalAmount(BigDecimal.valueOf(134.00))
                .paymentMethod("ONLINE")
                .paymentStatus("PAID")
                .items(new HashSet<>())
                .build();

        testItem = OrderItem.builder()
                .id(901L)
                .order(testOrder)
                .product(testProduct)
                .productNameSnapshot(testProduct.getName())
                .unitPrice(testProduct.getPrice())
                .quantity(3)
                .subtotal(BigDecimal.valueOf(99.00))
                .isPicked(false)
                .build();

        testOrder.getItems().add(testItem);

        testPartner = DeliveryPartner.builder()
                .id(201L)
                .user(riderUser)
                .isOnline(true)
                .status(DeliveryPartnerStatus.AVAILABLE)
                .totalEarnings(BigDecimal.ZERO)
                .build();

        testAssignment = DeliveryAssignment.builder()
                .id(8001L)
                .order(testOrder)
                .deliveryPartner(testPartner)
                .status(AssignmentStatus.ASSIGNED)
                .assignedAt(LocalDateTime.now())
                .build();

        lenient().when(storeRepository.findById(4L)).thenReturn(Optional.of(darkStore));
    }

    @Test
    @DisplayName("RBAC: Customer cannot access store operational queue")
    void getStoreQueue_CustomerCaller_DeniedWithAccessDeniedException() {
        assertThrows(AccessDeniedException.class, () ->
                storeOpsService.getStoreQueue(customerPrincipal, 4L));
    }

    @Test
    @DisplayName("Store Manager: Retrieve store queue with aggregated status metrics")
    void getStoreQueue_AuthorizedStaff_ReturnsQueueAndStatusCounts() {
        when(storeRepository.findById(4L)).thenReturn(Optional.of(darkStore));
        when(orderRepository.findByStoreIdOrderByCreatedAtDesc(4L)).thenReturn(List.of(testOrder));
        when(productRepository.findByStoreId(4L)).thenReturn(List.of(testProduct));
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(
                Inventory.builder().product(testProduct).quantity(2).lowStockThreshold(5).build()
        ));

        StoreQueueMetricsResponse queue = storeOpsService.getStoreQueue(storeManagerPrincipal, 4L);

        assertNotNull(queue);
        assertEquals(4L, queue.getStoreId());
        assertEquals(1, queue.getTotalOrders());
        assertEquals(1, queue.getConfirmedCount());
        assertEquals(0, queue.getPreparingCount());
        assertEquals(1, queue.getLowStockCount());
        assertEquals(1, queue.getOrders().size());
    }

    @Test
    @DisplayName("Picker Workflow: Start picking transitions order from CONFIRMED to PREPARING")
    void startPicking_ValidConfirmedOrder_TransitionsToPreparingAndAssignsPicker() {
        when(orderRepository.findById(5001L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse res = storeOpsService.startPicking(pickerPrincipal, 5001L);

        assertNotNull(res);
        assertEquals(OrderStatus.PREPARING.name(), res.getStatus());
        assertEquals(103L, testOrder.getPickerId());
        assertEquals("Picker Prem", testOrder.getPickerName());
        assertNotNull(testOrder.getPickingStartedAt());
    }

    @Test
    @DisplayName("Picker Workflow: Start picking rejected if order is not in CONFIRMED/PREPARING state")
    void startPicking_InvalidStatus_ThrowsIllegalStateException() {
        testOrder.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(5001L)).thenReturn(Optional.of(testOrder));

        assertThrows(IllegalStateException.class, () ->
                storeOpsService.startPicking(pickerPrincipal, 5001L));
    }

    @Test
    @DisplayName("Picker Workflow: Mark item picked toggles state and snapshots audit metadata")
    void pickItem_ValidOrderAndItem_TogglesPickedAndSnapshotsAudit() {
        when(orderRepository.findById(5001L)).thenReturn(Optional.of(testOrder));
        when(orderItemRepository.findById(901L)).thenReturn(Optional.of(testItem));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderItemResponse itemRes = storeOpsService.pickItem(pickerPrincipal, 5001L, 901L, new PickItemRequest());

        assertNotNull(itemRes);
        assertTrue(testItem.getIsPicked());
        assertEquals("Picker Prem", testItem.getPickedBy());
        assertNotNull(testItem.getPickedAt());
    }

    @Test
    @DisplayName("Picker Workflow: Complete picking rejected if unpicked items remain")
    void completePicking_UnpickedItemsRemaining_RejectsWithIllegalStateException() {
        testItem.setIsPicked(false);
        when(orderRepository.findById(5001L)).thenReturn(Optional.of(testOrder));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                storeOpsService.completePicking(pickerPrincipal, 5001L));

        assertTrue(ex.getMessage().contains("still pending verification"));
    }

    @Test
    @DisplayName("Packer Workflow: Complete packing transitions to READY and seals bag with barcode")
    void completePacking_ValidPickedOrder_TransitionsToReadyAndRecordsBagBarcode() {
        testOrder.setStatus(OrderStatus.PREPARING);
        testItem.setIsPicked(true);

        when(orderRepository.findById(5001L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        CompletePackingRequest req = new CompletePackingRequest("BAG-NX-7741");
        OrderResponse res = storeOpsService.completePacking(packerPrincipal, 5001L, req);

        assertNotNull(res);
        assertEquals(OrderStatus.READY.name(), res.getStatus());
        assertEquals("BAG-NX-7741", testOrder.getPackingBagCode());
        assertEquals(104L, testOrder.getPackerId());
        assertEquals("Packer Pooja", testOrder.getPackerName());
        assertNotNull(testOrder.getPackingCompletedAt());
        verify(deliveryService, times(1)).assignNearestRider(testOrder);
    }

    @Test
    @DisplayName("Inventory Safety: Stock adjustment preventing negative quantities")
    void adjustStock_NegativeResult_ThrowsIllegalArgumentException() {
        Inventory inv = Inventory.builder().product(testProduct).quantity(3).lowStockThreshold(5).build();
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inv));

        StoreStockAdjustmentRequest req = StoreStockAdjustmentRequest.builder()
                .productId(10L)
                .amount(10)
                .action("SUBTRACT")
                .reason("Damaged carton writeoff")
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                storeOpsService.adjustStock(storeManagerPrincipal, 4L, req));
    }

    @Test
    @DisplayName("Inventory Safety: Stock addition updates quantity and records audit")
    void adjustStock_ValidAdd_UpdatesQuantityAndLogsAudit() {
        Inventory inv = Inventory.builder().product(testProduct).quantity(10).lowStockThreshold(5).build();
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inv));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));

        StoreStockAdjustmentRequest req = StoreStockAdjustmentRequest.builder()
                .productId(10L)
                .amount(25)
                .action("ADD")
                .reason("Fresh morning delivery from central dairy hub")
                .build();

        var res = storeOpsService.adjustStock(storeManagerPrincipal, 4L, req);

        assertNotNull(res);
        assertEquals(35, res.getQuantity());
        assertEquals("IN_STOCK", res.getStatus());
    }
}
