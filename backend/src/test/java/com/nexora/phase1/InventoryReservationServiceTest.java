package com.nexora.phase1;

import com.nexora.inventory.entity.Inventory;
import com.nexora.inventory.entity.InventoryReservation;
import com.nexora.inventory.entity.ReservationStatus;
import com.nexora.inventory.exception.InsufficientStockException;
import com.nexora.inventory.repository.InventoryRepository;
import com.nexora.inventory.repository.InventoryReservationRepository;
import com.nexora.inventory.service.InventoryReservationService;
import com.nexora.order.entity.Order;
import com.nexora.order.entity.OrderItem;
import com.nexora.order.entity.OrderStatus;
import com.nexora.order.repository.OrderRepository;
import com.nexora.product.entity.Product;
import com.nexora.store.entity.InventoryBatch;
import com.nexora.store.entity.Store;
import com.nexora.store.repository.InventoryBatchRepository;
import com.nexora.store.service.ColdChainAndExpiryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 1: Two-Phase Inventory Reservation & FEFO Engine Tests")
class InventoryReservationServiceTest {

    @Mock private InventoryReservationRepository reservationRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private InventoryBatchRepository batchRepository;
    @Mock private ColdChainAndExpiryService coldChainService;
    @Mock private OrderRepository orderRepository;

    @InjectMocks
    private InventoryReservationService reservationService;

    private Store testStore;
    private Product testProduct;
    private InventoryBatch testBatch;
    private Order testOrder;
    private OrderItem testOrderItem;

    @BeforeEach
    void setUp() {
        testStore = Store.builder()
                .id(1L)
                .name("Ahmedabad Central Dark Store")
                .latitude(23.0225)
                .longitude(72.5714)
                .build();

        testProduct = Product.builder()
                .id(101L)
                .name("Amul Gold Milk 500ml")
                .price(BigDecimal.valueOf(33.00))
                .store(testStore)
                .build();

        testBatch = InventoryBatch.builder()
                .id(501L)
                .batchNumber("BATCH-AMUL-001")
                .product(testProduct)
                .store(testStore)
                .expiryDate(LocalDate.now().plusDays(2))
                .availableQuantity(10)
                .reservedQuantity(0)
                .build();

        testOrderItem = OrderItem.builder()
                .id(1L)
                .product(testProduct)
                .quantity(2)
                .unitPrice(testProduct.getPrice())
                .build();

        testOrder = Order.builder()
                .id(901L)
                .store(testStore)
                .status(OrderStatus.CREATED)
                .items(Set.of(testOrderItem))
                .build();
    }

    @Test
    @DisplayName("Workstream 1 & 3: Creates reservation with 10-minute sliding TTL and FEFO batch linkage")
    void testReservationCreationWithSliding10MinTtlAndFefoBatch() {
        when(inventoryRepository.reduceStockIfAvailable(eq(101L), eq(2), any(LocalDateTime.class)))
                .thenReturn(1);

        Map<InventoryBatch, Integer> batchMap = new HashMap<>();
        batchMap.put(testBatch, 2);
        when(coldChainService.allocateFefoStockDetailed(101L, 1L, 2)).thenReturn(batchMap);

        when(reservationRepository.save(any(InventoryReservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<InventoryReservation> reservations = reservationService.createReservationsForOrder(testOrder, 10);

        assertNotNull(reservations);
        assertEquals(1, reservations.size());
        InventoryReservation res = reservations.get(0);
        assertEquals(ReservationStatus.RESERVED, res.getStatus());
        assertEquals(2, res.getQuantity());
        assertEquals(testBatch, res.getBatch());
        assertNotNull(res.getExpiresAt());

        // Verifies 10-minute TTL window (between 9 and 11 minutes in future)
        LocalDateTime expectedMin = LocalDateTime.now().plusMinutes(9);
        LocalDateTime expectedMax = LocalDateTime.now().plusMinutes(11);
        assertTrue(res.getExpiresAt().isAfter(expectedMin) && res.getExpiresAt().isBefore(expectedMax));

        verify(inventoryRepository).reduceStockIfAvailable(eq(101L), eq(2), any(LocalDateTime.class));
        verify(coldChainService).allocateFefoStockDetailed(101L, 1L, 2);
    }

    @Test
    @DisplayName("Workstream 1: Prevents overselling when stock is insufficient (Throws InsufficientStockException)")
    void testPreventsOversellingWhenStockInsufficient() {
        when(inventoryRepository.reduceStockIfAvailable(eq(101L), eq(2), any(LocalDateTime.class)))
                .thenReturn(0); // 0 rows updated = insufficient stock

        assertThrows(InsufficientStockException.class, () ->
                reservationService.createReservationsForOrder(testOrder, 10));

        verify(reservationRepository, never()).save(any());
        verify(coldChainService, never()).allocateFefoStockDetailed(any(), any(), anyInt());
    }

    @Test
    @DisplayName("Workstream 1: Concurrent checkout race condition - only one purchase of last available stock succeeds")
    void testConcurrentCheckoutRaceConditionForLastStock() throws InterruptedException {
        // Product has only 1 item remaining in stock
        final AtomicInteger physicalStock = new AtomicInteger(1);

        when(inventoryRepository.reduceStockIfAvailable(eq(101L), eq(1), any(LocalDateTime.class)))
                .thenAnswer(inv -> {
                    // Atomic compare-and-swap simulates DB row lock condition: "WHERE quantity >= 1"
                    if (physicalStock.get() >= 1 && physicalStock.compareAndSet(1, 0)) {
                        return 1; // Success
                    }
                    return 0; // Failed (oversell blocked)
                });

        when(reservationRepository.save(any(InventoryReservation.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Two competing concurrent orders for the single item
        OrderItem item1 = OrderItem.builder().product(testProduct).quantity(1).build();
        Order order1 = Order.builder().id(1001L).store(testStore).items(Set.of(item1)).build();

        OrderItem item2 = OrderItem.builder().product(testProduct).quantity(1).build();
        Order order2 = Order.builder().id(1002L).store(testStore).items(Set.of(item2)).build();

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        executor.submit(() -> {
            try {
                startLatch.await();
                reservationService.createReservationsForOrder(order1, 10);
                successCount.incrementAndGet();
            } catch (InsufficientStockException ex) {
                failureCount.incrementAndGet();
            } catch (Exception ex) {
                // Unexpected
            } finally {
                doneLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                reservationService.createReservationsForOrder(order2, 10);
                successCount.incrementAndGet();
            } catch (InsufficientStockException ex) {
                failureCount.incrementAndGet();
            } catch (Exception ex) {
                // Unexpected
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown(); // Fire both threads simultaneously
        boolean completed = doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Concurrent threads must complete within 5 seconds");
        assertEquals(1, successCount.get(), "Exactly ONE checkout must succeed for the last stock");
        assertEquals(1, failureCount.get(), "The competing checkout must be blocked with InsufficientStockException");
        assertEquals(0, physicalStock.get(), "Stock must be exactly 0 after execution");
    }

    @Test
    @DisplayName("Workstream 1: Verified payment commits reservations permanently (RESERVED -> COMMITTED)")
    void testPaymentVerifiedCommitReservation() {
        InventoryReservation activeRes = InventoryReservation.builder()
                .id(1L)
                .order(testOrder)
                .product(testProduct)
                .batch(testBatch)
                .quantity(2)
                .status(ReservationStatus.RESERVED)
                .expiresAt(LocalDateTime.now().plusMinutes(8))
                .build();

        testBatch.setReservedQuantity(2);

        when(reservationRepository.findByOrderIdAndStatus(901L, ReservationStatus.RESERVED))
                .thenReturn(List.of(activeRes));

        reservationService.commitReservationsForOrder(901L);

        assertEquals(ReservationStatus.COMMITTED, activeRes.getStatus());
        assertEquals(0, testBatch.getReservedQuantity());
        verify(batchRepository).save(testBatch);
        verify(reservationRepository).save(activeRes);
    }

    @Test
    @DisplayName("Workstream 1: Order cancellation rolls back reservation and restores inventory (RESERVED -> RELEASED)")
    void testOrderCancellationRollbackReleasesInventory() {
        InventoryReservation activeRes = InventoryReservation.builder()
                .id(2L)
                .order(testOrder)
                .product(testProduct)
                .batch(testBatch)
                .quantity(2)
                .status(ReservationStatus.RESERVED)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        testBatch.setAvailableQuantity(8);
        testBatch.setReservedQuantity(2);

        when(reservationRepository.findByOrderIdAndStatus(901L, ReservationStatus.RESERVED))
                .thenReturn(List.of(activeRes));

        reservationService.releaseReservationsForOrder(901L, "Customer cancelled before payment");

        assertEquals(ReservationStatus.RELEASED, activeRes.getStatus());
        assertEquals(10, testBatch.getAvailableQuantity()); // Restored
        assertEquals(0, testBatch.getReservedQuantity());
        verify(inventoryRepository).increaseStock(eq(101L), eq(2), any(LocalDateTime.class));
        verify(batchRepository).save(testBatch);
        verify(reservationRepository).save(activeRes);
    }

    @Test
    @DisplayName("Workstream 1: Scheduled sweeper auto-expires stale reservations and cancels unpaid orders")
    void testScheduledSweeperExpiresStaleReservationsAndCancelsOrder() {
        InventoryReservation expiredRes = InventoryReservation.builder()
                .id(3L)
                .order(testOrder)
                .product(testProduct)
                .batch(testBatch)
                .quantity(2)
                .status(ReservationStatus.RESERVED)
                .expiresAt(LocalDateTime.now().minusMinutes(2)) // 2 minutes ago
                .build();

        testBatch.setAvailableQuantity(8);
        testBatch.setReservedQuantity(2);

        when(reservationRepository.findExpiredReservations(eq(ReservationStatus.RESERVED), any(LocalDateTime.class)))
                .thenReturn(List.of(expiredRes));
        when(orderRepository.findById(901L)).thenReturn(Optional.of(testOrder));

        reservationService.releaseExpiredReservations();

        assertEquals(ReservationStatus.EXPIRED, expiredRes.getStatus());
        assertEquals(10, testBatch.getAvailableQuantity()); // Restored
        assertEquals(0, testBatch.getReservedQuantity());
        assertEquals(OrderStatus.CANCELLED, testOrder.getStatus()); // Order auto-cancelled

        verify(inventoryRepository).increaseStock(eq(101L), eq(2), any(LocalDateTime.class));
        verify(orderRepository).save(testOrder);
        verify(reservationRepository).save(expiredRes);
    }
}
