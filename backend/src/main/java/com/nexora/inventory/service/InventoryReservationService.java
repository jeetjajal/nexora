package com.nexora.inventory.service;

import com.nexora.inventory.entity.InventoryReservation;
import com.nexora.inventory.entity.ReservationStatus;
import com.nexora.inventory.exception.InsufficientStockException;
import com.nexora.inventory.repository.InventoryRepository;
import com.nexora.inventory.repository.InventoryReservationRepository;
import com.nexora.order.entity.Order;
import com.nexora.order.entity.OrderItem;
import com.nexora.order.entity.OrderStatus;
import com.nexora.order.repository.OrderRepository;
import com.nexora.product.entity.Product;
import com.nexora.store.entity.InventoryBatch;
import com.nexora.store.repository.InventoryBatchRepository;
import com.nexora.store.service.ColdChainAndExpiryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * TWO-PHASE INVENTORY RESERVATION ENGINE (NEXORA 3.0)
 *
 * Implements a 10-minute sliding reservation TTL for concurrent checkouts.
 * - Phase 1 (RESERVED): Stock is atomically locked at checkout creation, binding FEFO batches.
 * - Phase 2 (COMMITTED): Verified payment marks stock permanently consumed.
 * - Rollback (RELEASED): Cancellation or payment failure restores stock immediately.
 * - Auto-Expiry (EXPIRED): Background scheduler auto-releases expired reservations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryReservationService {

    private final InventoryReservationRepository reservationRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryBatchRepository batchRepository;
    private final ColdChainAndExpiryService coldChainService;
    private final OrderRepository orderRepository;

    /**
     * Phase 1: Atomically locks inventory and binds FEFO batches for an order.
     * Default TTL: 10 minutes.
     */
    @Transactional
    public List<InventoryReservation> createReservationsForOrder(Order order, int ttlMinutes) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(ttlMinutes > 0 ? ttlMinutes : 10);
        List<InventoryReservation> createdReservations = new ArrayList<>();

        if (order.getItems() == null || order.getItems().isEmpty()) {
            return createdReservations;
        }

        Long storeId = order.getStore() != null ? order.getStore().getId() : 1L;

        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            int qty = item.getQuantity();

            // 1. Atomic check and decrement on master inventory (Row-level lock safety)
            int updated = inventoryRepository.reduceStockIfAvailable(product.getId(), qty, now);
            if (updated == 0) {
                log.warn("Oversell prevented: insufficient stock for product #{} ({})", product.getId(), product.getName());
                throw new InsufficientStockException(product.getId());
            }

            // 2. Batch-level FEFO allocation
            Map<InventoryBatch, Integer> batchAllocations = Collections.emptyMap();
            try {
                batchAllocations = coldChainService.allocateFefoStockDetailed(product.getId(), storeId, qty);
            } catch (Exception ex) {
                log.debug("Batch FEFO allocation deferred for SKU #{}: {}", product.getId(), ex.getMessage());
            }

            int allocatedFromBatches = 0;
            if (!batchAllocations.isEmpty()) {
                for (Map.Entry<InventoryBatch, Integer> entry : batchAllocations.entrySet()) {
                    InventoryBatch batch = entry.getKey();
                    int batchQty = entry.getValue();
                    allocatedFromBatches += batchQty;

                    InventoryReservation reservation = InventoryReservation.builder()
                            .order(order)
                            .product(product)
                            .batch(batch)
                            .store(order.getStore())
                            .quantity(batchQty)
                            .status(ReservationStatus.RESERVED)
                            .expiresAt(expiresAt)
                            .build();

                    createdReservations.add(reservationRepository.save(reservation));
                }
            }

            // If remaining quantity was not covered by batches, record general reservation
            int unbatchedQty = qty - allocatedFromBatches;
            if (unbatchedQty > 0) {
                InventoryReservation generalReservation = InventoryReservation.builder()
                        .order(order)
                        .product(product)
                        .batch(null)
                        .store(order.getStore())
                        .quantity(unbatchedQty)
                        .status(ReservationStatus.RESERVED)
                        .expiresAt(expiresAt)
                        .build();

                createdReservations.add(reservationRepository.save(generalReservation));
            }
        }

        log.info("Reserved inventory for order #{} across {} reservation records (Expires: {})",
                order.getId(), createdReservations.size(), expiresAt);
        return createdReservations;
    }

    /**
     * Phase 2: Commits reservations when payment is cryptographically verified.
     */
    @Transactional
    public void commitReservationsForOrder(Long orderId) {
        List<InventoryReservation> activeReservations =
                reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED);

        for (InventoryReservation res : activeReservations) {
            res.setStatus(ReservationStatus.COMMITTED);
            if (res.getBatch() != null) {
                InventoryBatch batch = res.getBatch();
                batch.setReservedQuantity(Math.max(0, batch.getReservedQuantity() - res.getQuantity()));
                batchRepository.save(batch);
            }
            reservationRepository.save(res);
        }

        log.info("Committed {} inventory reservations for order #{}", activeReservations.size(), orderId);
    }

    /**
     * Rollback: Releases reservations on order cancellation, returning stock to available pool.
     */
    @Transactional
    public void releaseReservationsForOrder(Long orderId, String reason) {
        List<InventoryReservation> activeReservations =
                reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED);

        for (InventoryReservation res : activeReservations) {
            res.setStatus(ReservationStatus.RELEASED);

            // Restore master inventory
            inventoryRepository.increaseStock(res.getProduct().getId(), res.getQuantity(), LocalDateTime.now());

            // Restore batch inventory if linked
            if (res.getBatch() != null) {
                InventoryBatch batch = res.getBatch();
                batch.setAvailableQuantity(batch.getAvailableQuantity() + res.getQuantity());
                batch.setReservedQuantity(Math.max(0, batch.getReservedQuantity() - res.getQuantity()));
                batchRepository.save(batch);
            }

            reservationRepository.save(res);
        }

        log.info("Released {} inventory reservations for order #{} (Reason: {})",
                activeReservations.size(), orderId, reason);
    }

    /**
     * Scheduled Background Sweeper:
     * Sweeps every 60 seconds to release any reservations whose 10-minute TTL expired.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void releaseExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<InventoryReservation> expiredList =
                reservationRepository.findExpiredReservations(ReservationStatus.RESERVED, now);

        if (expiredList.isEmpty()) {
            return;
        }

        log.info("Processing {} expired inventory reservations at {}", expiredList.size(), now);
        Set<Long> affectedOrderIds = new HashSet<>();

        for (InventoryReservation res : expiredList) {
            res.setStatus(ReservationStatus.EXPIRED);

            // Restore master inventory
            inventoryRepository.increaseStock(res.getProduct().getId(), res.getQuantity(), now);

            // Restore batch inventory
            if (res.getBatch() != null) {
                InventoryBatch batch = res.getBatch();
                batch.setAvailableQuantity(batch.getAvailableQuantity() + res.getQuantity());
                batch.setReservedQuantity(Math.max(0, batch.getReservedQuantity() - res.getQuantity()));
                batchRepository.save(batch);
            }

            reservationRepository.save(res);
            if (res.getOrder() != null) {
                affectedOrderIds.add(res.getOrder().getId());
            }
        }

        // Cancel unpaid orders whose reservations expired
        for (Long orderId : affectedOrderIds) {
            orderRepository.findById(orderId).ifPresent(order -> {
                if (order.getStatus() == OrderStatus.CREATED || order.getStatus() == OrderStatus.PAYMENT_PENDING) {
                    order.setStatus(OrderStatus.CANCELLED);
                    orderRepository.save(order);
                    log.warn("Order #{} auto-cancelled: 10-minute payment reservation expired.", orderId);
                }
            });
        }
    }
}
