package com.nexora.wms;

import com.nexora.delivery.entity.DeliveryPartner;
import com.nexora.delivery.repository.DeliveryPartnerRepository;
import com.nexora.delivery.service.DeliveryService;
import com.nexora.exception.BadRequestException;
import com.nexora.inventory.entity.InventoryReservation;
import com.nexora.inventory.repository.InventoryReservationRepository;
import com.nexora.order.entity.Order;
import com.nexora.order.entity.OrderItem;
import com.nexora.order.entity.OrderStatus;
import com.nexora.order.repository.OrderRepository;
import com.nexora.product.entity.Product;
import com.nexora.store.entity.InventoryBatch;
import com.nexora.store.entity.Store;
import com.nexora.store.repository.InventoryBatchRepository;
import com.nexora.store.repository.StoreRepository;
import com.nexora.user.entity.User;
import com.nexora.wms.dto.CompletePackingWmsRequest;
import com.nexora.wms.dto.DispatchHandoverRequest;
import com.nexora.wms.dto.DispatchHandoverResponse;
import com.nexora.wms.dto.ScanItemPickRequest;
import com.nexora.wms.dto.ScanItemPickResponse;
import com.nexora.wms.entity.*;
import com.nexora.wms.enums.*;
import com.nexora.wms.repository.*;
import com.nexora.wms.service.BarcodeScanningService;
import com.nexora.wms.service.PickPathOptimizationService;
import com.nexora.wms.service.PickerAssignmentService;
import com.nexora.wms.service.WmsFulfillmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 3: WS4, WS5, WS6 — Picker Assignment & WMS Fulfillment Integration Tests")
class PickerAssignmentAndWmsFulfillmentIntegrationTest {

    @Mock private OrderRepository orderRepository;
    @Mock private PickTaskRepository pickTaskRepository;
    @Mock private PickTaskItemRepository pickTaskItemRepository;
    @Mock private PickerProfileRepository pickerProfileRepository;
    @Mock private PickingEventRepository pickingEventRepository;
    @Mock private PackingTaskRepository packingTaskRepository;
    @Mock private DispatchHandoverRepository dispatchHandoverRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private StoreBinRepository binRepository;
    @Mock private ProductStorageLocationRepository productLocationRepository;
    @Mock private InventoryReservationRepository inventoryReservationRepository;
    @Mock private InventoryBatchRepository batchRepository;
    @Mock private DeliveryPartnerRepository deliveryPartnerRepository;

    @Mock private PickPathOptimizationService pickPathOptimizationService;
    @Mock private PickerAssignmentService pickerAssignmentService;
    @Mock private BarcodeScanningService barcodeScanningService;
    @Mock private DeliveryService deliveryService;

    @InjectMocks
    private WmsFulfillmentService fulfillmentService;

    private Store mockStore;
    private User mockUser;
    private Order mockOrder;
    private Product mockProduct;
    private InventoryBatch mockBatch;
    private PickerProfile mockPicker;
    private StoreBin mockBin;
    private PickTask mockTask;
    private PickTaskItem mockTaskItem;
    private DeliveryPartner mockRider;

    @BeforeEach
    void setUp() {
        mockStore = new Store();
        mockStore.setId(4L);
        mockStore.setName("SG Highway Dark Store");

        mockUser = new User();
        mockUser.setId(10L);
        mockUser.setName("Rajesh Sharma");

        mockOrder = new Order();
        mockOrder.setId(1001L);
        mockOrder.setStore(mockStore);
        mockOrder.setUser(mockUser);
        mockOrder.setStatus(OrderStatus.CONFIRMED);

        mockProduct = new Product();
        mockProduct.setId(501L);
        mockProduct.setName("Amul Butter 500g");
        mockProduct.setSku("SKU-AMUL-500");
        mockProduct.setPrimaryBarcode("8901262010052");

        mockBatch = InventoryBatch.builder()
                .id(701L)
                .batchNumber("BATCH-2026-A")
                .expiryDate(LocalDate.now().plusMonths(3))
                .availableQuantity(40)
                .build();

        mockBin = StoreBin.builder()
                .id(801L)
                .fullLocationCode("ST04-ZAMB-A01-R01-S01-B01")
                .build();

        mockPicker = PickerProfile.builder()
                .id(901L)
                .employeeCode("PKR-001")
                .status(PickerStatus.AVAILABLE)
                .store(mockStore)
                .completedTasksCount(2)
                .build();

        mockRider = new DeliveryPartner();
        mockRider.setId(888L);
        User riderUser = new User();
        riderUser.setName("Vikram Patel");
        mockRider.setUser(riderUser);

        OrderItem orderItem = new OrderItem();
        orderItem.setId(2001L);
        orderItem.setOrder(mockOrder);
        orderItem.setProduct(mockProduct);
        orderItem.setQuantity(2);
        mockOrder.setItems(new HashSet<>(List.of(orderItem)));

        mockTask = PickTask.builder()
                .id(3001L)
                .taskCode("PT-1001")
                .order(mockOrder)
                .store(mockStore)
                .picker(mockPicker)
                .status(PickTaskStatus.PENDING)
                .items(new ArrayList<>())
                .build();

        mockTaskItem = PickTaskItem.builder()
                .id(4001L)
                .pickTask(mockTask)
                .orderItem(orderItem)
                .product(mockProduct)
                .bin(mockBin)
                .batch(mockBatch)
                .requiredQuantity(2)
                .pickedQuantity(0)
                .status(PickItemStatus.PENDING)
                .build();

        mockTask.getItems().add(mockTaskItem);
    }

    @Test
    @DisplayName("WS4: Generate PickTask binds FEFO batches, S-Shape sequence, and assigns picker")
    void testGeneratePickTaskSuccess() {
        when(pickTaskRepository.findByOrderId(1001L)).thenReturn(Optional.empty());

        InventoryReservation res = InventoryReservation.builder()
                .order(mockOrder)
                .product(mockProduct)
                .batch(mockBatch)
                .build();
        when(inventoryReservationRepository.findByOrderId(1001L)).thenReturn(List.of(res));

        ProductStorageLocation psl = ProductStorageLocation.builder()
                .product(mockProduct)
                .bin(mockBin)
                .isPrimary(true)
                .build();
        when(productLocationRepository.findFirstByProductIdAndStoreIdAndIsPrimaryTrue(501L, 4L))
                .thenReturn(Optional.of(psl));

        PickPathOptimizationService.OptimizedRouteResult routeResult = PickPathOptimizationService.OptimizedRouteResult.builder()
                .orderedItems(List.of(mockTaskItem))
                .totalDistanceMeters(18.5)
                .estimatedDurationSeconds(35)
                .selectedAlgorithm(PickPathAlgorithm.S_SHAPE)
                .build();
        when(pickPathOptimizationService.optimizePickRoute(anyList())).thenReturn(routeResult);

        when(pickTaskRepository.save(any(PickTask.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pickerAssignmentService.assignPickerToTask(any(PickTask.class))).thenReturn(Optional.of(mockPicker));

        PickTask generated = fulfillmentService.generatePickTask(mockOrder);

        assertNotNull(generated);
        assertEquals(PickTaskStatus.PENDING, generated.getStatus());
        assertEquals(OrderStatus.PREPARING, mockOrder.getStatus());
        verify(pickerAssignmentService, times(1)).assignPickerToTask(any(PickTask.class));
        verify(orderRepository, times(1)).save(mockOrder);
    }

    @Test
    @DisplayName("WS2 & WS6: Scanned barcode succeeds and guards against over-picking")
    void testScanItemPickSuccessAndOverPickGuard() {
        when(pickTaskItemRepository.findById(4001L)).thenReturn(Optional.of(mockTaskItem));
        when(barcodeScanningService.resolveProduct("8901262010052")).thenReturn(Optional.of(mockProduct));
        when(pickTaskRepository.save(any(PickTask.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pickTaskItemRepository.save(any(PickTaskItem.class))).thenAnswer(inv -> inv.getArgument(0));

        // 1. Pick 1 unit out of 2
        ScanItemPickRequest req1 = ScanItemPickRequest.builder()
                .scannedBarcode("8901262010052")
                .quantity(1)
                .build();

        ScanItemPickResponse resp1 = fulfillmentService.scanItemPick(4001L, req1, 10L);
        assertNotNull(resp1);
        assertEquals(1, resp1.getPickedQuantity());
        assertFalse(resp1.getIsFullyPicked());

        // 2. Pick remaining 1 unit to complete
        ScanItemPickRequest req2 = ScanItemPickRequest.builder()
                .scannedBarcode("8901262010052")
                .quantity(1)
                .build();

        ScanItemPickResponse resp2 = fulfillmentService.scanItemPick(4001L, req2, 10L);
        assertNotNull(resp2);
        assertEquals(2, resp2.getPickedQuantity());
        assertTrue(resp2.getIsFullyPicked());

        // 3. Over-pick guard: attempting to pick 1 more unit must throw BadRequestException
        ScanItemPickRequest overpickReq = ScanItemPickRequest.builder()
                .scannedBarcode("8901262010052")
                .quantity(1)
                .build();

        assertThrows(BadRequestException.class, () ->
                fulfillmentService.scanItemPick(4001L, overpickReq, 10L));
    }

    @Test
    @DisplayName("WS2: Barcode mismatch throws BadRequestException and logs audit event")
    void testScanItemBarcodeMismatch() {
        when(pickTaskItemRepository.findById(4001L)).thenReturn(Optional.of(mockTaskItem));

        Product wrongProd = new Product();
        wrongProd.setId(999L);
        wrongProd.setName("Wrong Soda 500ml");
        when(barcodeScanningService.resolveProduct("WRONG_BARCODE")).thenReturn(Optional.of(wrongProd));

        ScanItemPickRequest wrongReq = ScanItemPickRequest.builder()
                .scannedBarcode("WRONG_BARCODE")
                .quantity(1)
                .build();

        assertThrows(BadRequestException.class, () ->
                fulfillmentService.scanItemPick(4001L, wrongReq, 10L));

        verify(pickingEventRepository, times(1)).save(any(PickingEvent.class));
    }

    @Test
    @DisplayName("WS6: Expired batch safety guard blocks picking")
    void testExpiredBatchSafetyGuard() {
        InventoryBatch expiredBatch = InventoryBatch.builder()
                .id(702L)
                .batchNumber("EXP-2025")
                .expiryDate(LocalDate.now().minusDays(5)) // Expired!
                .build();
        mockTaskItem.setBatch(expiredBatch);

        when(pickTaskItemRepository.findById(4001L)).thenReturn(Optional.of(mockTaskItem));
        when(barcodeScanningService.resolveProduct("8901262010052")).thenReturn(Optional.of(mockProduct));

        ScanItemPickRequest req = ScanItemPickRequest.builder()
                .scannedBarcode("8901262010052")
                .quantity(1)
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                fulfillmentService.scanItemPick(4001L, req, 10L));

        assertTrue(ex.getMessage().contains("Expired batch blocked by FEFO safety guard"));
    }

    @Test
    @DisplayName("WS5: Complete packing seals bag, advances order to READY, and triggers Phase 2 nearest rider assignment")
    void testCompletePackingAndRiderDispatchTrigger() {
        mockTaskItem.setStatus(PickItemStatus.PICKED);
        mockTaskItem.setPickedQuantity(2);
        mockTask.setStatus(PickTaskStatus.COMPLETED);

        when(orderRepository.findById(1001L)).thenReturn(Optional.of(mockOrder));
        when(pickTaskRepository.findByOrderId(1001L)).thenReturn(Optional.of(mockTask));
        when(packingTaskRepository.findByOrderId(1001L)).thenReturn(Optional.empty());
        when(packingTaskRepository.save(any(PackingTask.class))).thenAnswer(inv -> inv.getArgument(0));

        CompletePackingWmsRequest packReq = CompletePackingWmsRequest.builder()
                .bagCode("BAG-1001-A")
                .qcPassed(true)
                .build();

        PackingTask packed = fulfillmentService.completePacking(1001L, packReq, 55L);

        assertNotNull(packed);
        assertEquals("BAG-1001-A", packed.getBagCode());
        assertEquals(PackingTaskStatus.COMPLETED, packed.getStatus());
        assertEquals(OrderStatus.READY, mockOrder.getStatus());
        assertEquals("BAG-1001-A", mockOrder.getPackingBagCode());

        // Verify Phase 2 DeliveryService.assignNearestRider was triggered!
        verify(deliveryService, times(1)).assignNearestRider(mockOrder);
    }

    @Test
    @DisplayName("WS5: Dispatch handover creates handover record and advances order to PICKED_UP")
    void testDispatchHandoverSuccess() {
        mockOrder.setStatus(OrderStatus.RIDER_ASSIGNED);

        when(orderRepository.findById(1001L)).thenReturn(Optional.of(mockOrder));
        when(deliveryPartnerRepository.findById(888L)).thenReturn(Optional.of(mockRider));
        when(dispatchHandoverRepository.save(any(DispatchHandover.class))).thenAnswer(inv -> {
            DispatchHandover dh = inv.getArgument(0);
            dh.setId(7771L);
            return dh;
        });

        DispatchHandoverRequest req = DispatchHandoverRequest.builder()
                .deliveryPartnerId(888L)
                .bagCode("BAG-1001-A")
                .build();

        DispatchHandoverResponse resp = fulfillmentService.handoverToDispatch(1001L, req, 10L);

        assertNotNull(resp);
        assertEquals(7771L, resp.getHandoverId());
        assertEquals(OrderStatus.PICKED_UP.name(), resp.getOrderStatus());
        assertEquals(OrderStatus.PICKED_UP, mockOrder.getStatus());
        verify(orderRepository, times(1)).save(mockOrder);
    }
}
