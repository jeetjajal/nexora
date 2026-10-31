package com.nexora.delivery;

import com.nexora.delivery.dto.DeliveryBatchResponse;
import com.nexora.delivery.entity.*;
import com.nexora.delivery.repository.DeliveryAssignmentRepository;
import com.nexora.delivery.repository.DeliveryBatchOrderRepository;
import com.nexora.delivery.repository.DeliveryBatchRepository;
import com.nexora.delivery.repository.DeliveryPartnerRepository;
import com.nexora.delivery.service.DeliveryBatchingService;
import com.nexora.address.entity.Address;
import com.nexora.order.entity.Order;
import com.nexora.order.entity.OrderStatus;
import com.nexora.order.repository.OrderRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryBatchingServiceTest {

    @Mock
    private DeliveryBatchRepository batchRepository;

    @Mock
    private DeliveryBatchOrderRepository batchOrderRepository;

    @Mock
    private DeliveryPartnerRepository partnerRepository;

    @Mock
    private DeliveryAssignmentRepository assignmentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private DeliveryBatchingService batchingService;

    private Store testStore;
    private Order order1;
    private Order order2;
    private Order distantOrder;
    private DeliveryPartner testPartner;

    @BeforeEach
    void setUp() {
        testStore = Store.builder()
                .id(1L)
                .name("SG Highway Dark Store Hub")
                .description("Ahmedabad Fulfilment [GEO: 23.0373, 72.5118, 3.5km]")
                .build();

        User user1 = User.builder().id(101L).name("Pooja Shah").phone("9876543210").build();
        User user2 = User.builder().id(102L).name("Rahul Patel").phone("9876543211").build();
        User user3 = User.builder().id(103L).name("Faraway Customer").phone("9876543212").build();

        Address addr1 = Address.builder()
                .id(201L)
                .addressLine1("Block B, Iscon Heights")
                .city("Ahmedabad")
                .latitude(23.0340)
                .longitude(72.5080) // ~0.5 km from store
                .build();

        Address addr2 = Address.builder()
                .id(202L)
                .addressLine1("A-302, Satellite Plaza")
                .city("Ahmedabad")
                .latitude(23.0320)
                .longitude(72.5060) // ~0.3 km from addr1
                .build();

        Address addrDistant = Address.builder()
                .id(203L)
                .addressLine1("Naroda GIDC")
                .city("Ahmedabad")
                .latitude(23.0800)
                .longitude(72.6500) // ~16 km away
                .build();

        order1 = Order.builder()
                .id(1001L)
                .store(testStore)
                .user(user1)
                .deliveryAddress(addr1)
                .status(OrderStatus.READY)
                .totalAmount(new BigDecimal("340.00"))
                .build();

        order2 = Order.builder()
                .id(1002L)
                .store(testStore)
                .user(user2)
                .deliveryAddress(addr2)
                .status(OrderStatus.READY)
                .totalAmount(new BigDecimal("520.00"))
                .build();

        distantOrder = Order.builder()
                .id(1003L)
                .store(testStore)
                .user(user3)
                .deliveryAddress(addrDistant)
                .status(OrderStatus.READY)
                .totalAmount(new BigDecimal("780.00"))
                .build();

        testPartner = DeliveryPartner.builder()
                .id(501L)
                .user(User.builder().id(301L).name("Vikram Rider").phone("9123456789").build())
                .status(DeliveryPartnerStatus.AVAILABLE)
                .isOnline(true)
                .build();
    }

    @Test
    @DisplayName("Haversine formula calculates accurate geodesic distance between coordinates")
    void testCalculateHaversineDistance() {
        // Distance between SG Highway (23.0373, 72.5118) and Iscon (23.0340, 72.5080)
        double distance = batchingService.calculateHaversineDistance(23.0373, 72.5118, 23.0340, 72.5080);
        assertTrue(distance > 0.4 && distance < 0.8, "Expected distance around 0.53 km, got " + distance);
    }

    @Test
    @DisplayName("generateBatches groups nearby orders and excludes distant orders outside proximity threshold")
    void testGenerateBatchesClustersNearbyOrders() {
        when(storeRepository.findById(1L)).thenReturn(Optional.of(testStore));
        when(orderRepository.findByStoreIdAndStatusInOrderByCreatedAtAsc(eq(1L), anyList()))
                .thenReturn(List.of(order1, order2, distantOrder));
        when(batchOrderRepository.existsByOrderId(anyLong())).thenReturn(false);

        when(batchRepository.save(any(DeliveryBatch.class))).thenAnswer(invocation -> {
            DeliveryBatch b = invocation.getArgument(0);
            b.setId(88L);
            b.setCreatedAt(LocalDateTime.now());
            b.setUpdatedAt(LocalDateTime.now());
            return b;
        });

        when(batchOrderRepository.findByBatchIdOrderBySequenceIndexAsc(88L)).thenReturn(List.of(
                DeliveryBatchOrder.builder().id(1L).sequenceIndex(1).order(order1).status("PENDING").build(),
                DeliveryBatchOrder.builder().id(2L).sequenceIndex(2).order(order2).status("PENDING").build()
        ));

        List<DeliveryBatchResponse> batches = batchingService.generateBatchesForStore(1L, 3, 2.5);

        assertNotNull(batches);
        assertEquals(1, batches.size());
        DeliveryBatchResponse batch = batches.get(0);
        assertEquals(2, batch.getOrderCount());
        assertEquals("SG Highway Dark Store Hub", batch.getStoreName());
        assertEquals(DeliveryBatchStatus.CREATED, batch.getStatus());
        assertEquals(2, batch.getStops().size());
        assertEquals(1001L, batch.getStops().get(0).getOrderId());
        assertEquals(1002L, batch.getStops().get(1).getOrderId());

        verify(batchRepository, times(1)).save(any(DeliveryBatch.class));
        verify(batchOrderRepository, times(2)).save(any(DeliveryBatchOrder.class));
    }

    @Test
    @DisplayName("assignBatchToRider sets partner and updates orders to RIDER_ASSIGNED")
    void testAssignBatchToRider() {
        DeliveryBatch batch = DeliveryBatch.builder()
                .id(88L)
                .batchCode("BATCH-1-12345")
                .store(testStore)
                .status(DeliveryBatchStatus.CREATED)
                .orderCount(2)
                .totalDistanceKm(new BigDecimal("1.20"))
                .estimatedDurationMins(12)
                .batchOrders(new ArrayList<>())
                .build();

        DeliveryBatchOrder bo1 = DeliveryBatchOrder.builder().id(1L).batch(batch).order(order1).sequenceIndex(1).status("PENDING").build();
        DeliveryBatchOrder bo2 = DeliveryBatchOrder.builder().id(2L).batch(batch).order(order2).sequenceIndex(2).status("PENDING").build();

        when(batchRepository.findById(88L)).thenReturn(Optional.of(batch));
        when(partnerRepository.findById(501L)).thenReturn(Optional.of(testPartner));
        when(batchOrderRepository.findByBatchIdOrderBySequenceIndexAsc(88L)).thenReturn(List.of(bo1, bo2));

        DeliveryBatchResponse response = batchingService.assignBatchToRider(88L, 501L);

        assertEquals(DeliveryBatchStatus.ASSIGNED, response.getStatus());
        assertEquals(501L, response.getDeliveryPartnerId());
        assertEquals("Vikram Rider", response.getRiderName());
        assertEquals(OrderStatus.RIDER_ASSIGNED, order1.getStatus());
        assertEquals(OrderStatus.RIDER_ASSIGNED, order2.getStatus());

        verify(assignmentRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("dispatchBatch sets status to IN_TRANSIT and orders to OUT_FOR_DELIVERY")
    void testDispatchBatch() {
        DeliveryBatch batch = DeliveryBatch.builder()
                .id(88L)
                .batchCode("BATCH-1-12345")
                .store(testStore)
                .deliveryPartner(testPartner)
                .status(DeliveryBatchStatus.ASSIGNED)
                .orderCount(2)
                .totalDistanceKm(new BigDecimal("1.20"))
                .estimatedDurationMins(12)
                .batchOrders(new ArrayList<>())
                .build();

        DeliveryBatchOrder bo1 = DeliveryBatchOrder.builder().id(1L).batch(batch).order(order1).sequenceIndex(1).status("PENDING").build();
        DeliveryBatchOrder bo2 = DeliveryBatchOrder.builder().id(2L).batch(batch).order(order2).sequenceIndex(2).status("PENDING").build();

        when(batchRepository.findById(88L)).thenReturn(Optional.of(batch));
        when(batchOrderRepository.findByBatchIdOrderBySequenceIndexAsc(88L)).thenReturn(List.of(bo1, bo2));

        DeliveryBatchResponse response = batchingService.dispatchBatch(88L);

        assertEquals(DeliveryBatchStatus.IN_TRANSIT, response.getStatus());
        assertNotNull(batch.getDispatchedAt());
        assertEquals(OrderStatus.OUT_FOR_DELIVERY, order1.getStatus());
        assertEquals(OrderStatus.OUT_FOR_DELIVERY, order2.getStatus());
    }

    @Test
    @DisplayName("cancelBatch reverts status to CANCELLED and restores orders to READY")
    void testCancelBatch() {
        order1.setStatus(OrderStatus.RIDER_ASSIGNED);
        order2.setStatus(OrderStatus.RIDER_ASSIGNED);

        DeliveryBatch batch = DeliveryBatch.builder()
                .id(88L)
                .batchCode("BATCH-1-12345")
                .store(testStore)
                .deliveryPartner(testPartner)
                .status(DeliveryBatchStatus.ASSIGNED)
                .orderCount(2)
                .totalDistanceKm(new BigDecimal("1.20"))
                .estimatedDurationMins(12)
                .batchOrders(new ArrayList<>())
                .build();

        DeliveryBatchOrder bo1 = DeliveryBatchOrder.builder().id(1L).batch(batch).order(order1).sequenceIndex(1).status("PENDING").build();
        DeliveryBatchOrder bo2 = DeliveryBatchOrder.builder().id(2L).batch(batch).order(order2).sequenceIndex(2).status("PENDING").build();

        when(batchRepository.findById(88L)).thenReturn(Optional.of(batch));
        when(batchOrderRepository.findByBatchIdOrderBySequenceIndexAsc(88L)).thenReturn(List.of(bo1, bo2));

        DeliveryBatchResponse response = batchingService.cancelBatch(88L);

        assertEquals(DeliveryBatchStatus.CANCELLED, response.getStatus());
        assertEquals(OrderStatus.READY, order1.getStatus());
        assertEquals(OrderStatus.READY, order2.getStatus());
    }
}
