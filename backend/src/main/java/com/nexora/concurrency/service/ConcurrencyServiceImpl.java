package com.nexora.concurrency.service;

import com.nexora.concurrency.dto.ConcurrencySimulationRequest;
import com.nexora.concurrency.dto.ConcurrencySimulationResult;
import com.nexora.concurrency.dto.ConcurrencyStrategyInfoResponse;
import com.nexora.concurrency.dto.FlashSalePurchaseRequest;
import com.nexora.concurrency.dto.FlashSalePurchaseResponse;
import com.nexora.concurrency.model.ConcurrencyStrategy;
import com.nexora.exception.ResourceNotFoundException;
import com.nexora.inventory.entity.Inventory;
import com.nexora.inventory.repository.InventoryRepository;
import com.nexora.redis.service.RedisDistributedLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of Concurrency and Race Condition Engine for Phase 21.
 * Supports comparison and execution of:
 *   1. Pessimistic Locking (SELECT ... FOR UPDATE)
 *   2. Optimistic Locking (@Version)
 *   3. Atomic Conditional SQL UPDATE
 *   4. Redis Distributed Locking
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConcurrencyServiceImpl implements ConcurrencyService {

    private final InventoryRepository inventoryRepository;
    private final ConcurrencyTransactionManager transactionManager;
    private final RedisDistributedLockService redisDistributedLockService;

    @Override
    public FlashSalePurchaseResponse purchaseFlashSale(FlashSalePurchaseRequest request) {
        long startTime = System.currentTimeMillis();
        Long productId = request.getProductId();
        int quantity = request.getQuantity();
        ConcurrencyStrategy strategy = request.getStrategy() != null ? request.getStrategy() : ConcurrencyStrategy.PESSIMISTIC_LOCK;

        try {
            int remainingStock = executeByStrategy(productId, quantity, strategy);
            long elapsed = System.currentTimeMillis() - startTime;
            return FlashSalePurchaseResponse.builder()
                    .success(true)
                    .productId(productId)
                    .remainingStock(remainingStock)
                    .strategyUsed(strategy)
                    .executionTimeMs(elapsed)
                    .message("Purchase successful! Remaining stock: " + remainingStock)
                    .build();
        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - startTime;
            int currentStock = inventoryRepository.findByProductId(productId)
                    .map(Inventory::getQuantity)
                    .orElse(0);

            return FlashSalePurchaseResponse.builder()
                    .success(false)
                    .productId(productId)
                    .remainingStock(currentStock)
                    .strategyUsed(strategy)
                    .executionTimeMs(elapsed)
                    .message("Purchase failed: " + ex.getMessage())
                    .build();
        }
    }

    @Override
    public ConcurrencySimulationResult simulateConcurrentFlashSale(ConcurrencySimulationRequest request) {
        Long productId = request.getProductId();
        int initialStock = request.getInitialStock();
        int totalThreads = request.getTotalThreads();
        ConcurrencyStrategy strategy = request.getStrategy() != null ? request.getStrategy() : ConcurrencyStrategy.PESSIMISTIC_LOCK;

        // 1. Reset stock to initial benchmark quantity in an isolated transaction
        transactionManager.resetStock(productId, initialStock);

        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicInteger failureCounter = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch readyLatch = new CountDownLatch(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < totalThreads; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    // Block until all threads are created, then release all simultaneously!
                    startLatch.await();
                    executeByStrategy(productId, 1, strategy);
                    successCounter.incrementAndGet();
                } catch (Exception ex) {
                    log.error("Simulation worker thread error: {}", ex.getMessage());
                    failureCounter.incrementAndGet();
                }
            });
        }

        try {
            // Wait for all worker threads to reach starting line
            readyLatch.await(10, TimeUnit.SECONDS);
            // FIRE all concurrent requests simultaneously!
            startLatch.countDown();

            executor.shutdown();
            boolean terminated = executor.awaitTermination(30, TimeUnit.SECONDS);
            if (!terminated) {
                log.warn("Simulation executor timed out waiting for all tasks to terminate.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long durationMs = Math.max(1, System.currentTimeMillis() - startTime);

        int finalStock = transactionManager.getFreshStock(productId);

        int totalRequests = successCounter.get() + failureCounter.get();
        double throughput = (totalRequests * 1000.0) / durationMs;

        // Verify that inventory never went negative and exactly initialStock items were bought
        boolean oversellingPrevented = finalStock >= 0 && (successCounter.get() <= initialStock);

        String explanation = String.format(
                "Executed %d concurrent purchases using %s. Initial Stock: %d, Sold: %d, Final Stock: %d, Rejected (Out of Stock): %d. Overselling prevented: %b.",
                totalRequests, strategy, initialStock, successCounter.get(), finalStock, failureCounter.get(), oversellingPrevented
        );

        return ConcurrencySimulationResult.builder()
                .productId(productId)
                .strategy(strategy)
                .initialStock(initialStock)
                .finalStock(finalStock)
                .totalRequests(totalRequests)
                .successfulPurchases(successCounter.get())
                .failedPurchases(failureCounter.get())
                .totalDurationMs(durationMs)
                .throughputReqPerSec(Math.round(throughput * 10.0) / 10.0)
                .oversellingPrevented(oversellingPrevented)
                .explanation(explanation)
                .build();
    }

    @Override
    public List<ConcurrencyStrategyInfoResponse> getStrategyComparisons() {
        List<ConcurrencyStrategyInfoResponse> list = new ArrayList<>();

        list.add(ConcurrencyStrategyInfoResponse.builder()
                .strategy(ConcurrencyStrategy.ATOMIC_UPDATE)
                .name("Atomic Conditional SQL UPDATE")
                .mechanism("Single SQL statement: UPDATE inventory SET quantity = quantity - :amount WHERE quantity >= :amount")
                .isolationLevel("READ_COMMITTED (Database Row-Level Lock)")
                .bestUsedFor("Standard e-commerce checkout. Extremely fast, minimal overhead, zero deadlock risk.")
                .tradeOffs("Limited to single-table quantity checks. Cannot perform complex multi-entity validation inside Java.")
                .build());

        list.add(ConcurrencyStrategyInfoResponse.builder()
                .strategy(ConcurrencyStrategy.PESSIMISTIC_LOCK)
                .name("Pessimistic Write Locking")
                .mechanism("SELECT ... FOR UPDATE via @Lock(LockModeType.PESSIMISTIC_WRITE)")
                .isolationLevel("READ_COMMITTED / REPEATABLE_READ")
                .bestUsedFor("High-contention Flash Sales (e.g. stock=1 and 100+ simultaneous buyers). Guarantees ordered execution.")
                .tradeOffs("Holds database row lock during transaction; can cause database thread pool contention if held too long.")
                .build());

        list.add(ConcurrencyStrategyInfoResponse.builder()
                .strategy(ConcurrencyStrategy.OPTIMISTIC_LOCK)
                .name("Optimistic Locking with @Version")
                .mechanism("Hibernate @Version column check on UPDATE. Throws ObjectOptimisticLockingFailureException on conflict.")
                .isolationLevel("READ_COMMITTED")
                .bestUsedFor("Low-to-medium contention systems where reads vastly outnumber writes and conflicts are rare.")
                .tradeOffs("Under intense flash sales (100 simultaneous buyers on stock 1), 99 transactions abort and roll back, causing retry storm.")
                .build());

        list.add(ConcurrencyStrategyInfoResponse.builder()
                .strategy(ConcurrencyStrategy.DISTRIBUTED_LOCK)
                .name("Redis Distributed Locking")
                .mechanism("Redis SETNX with TTL token lease to serialize checkout before hitting MySQL.")
                .isolationLevel("Application / Distributed Cache Layer")
                .bestUsedFor("Multi-instance microservice clusters. Protects database from connection exhaustion during mega flash sales.")
                .tradeOffs("Introduces network hop to Redis. Requires fail-safe lease expiry to avoid permanent deadlocks if a server crashes.")
                .build());

        return list;
    }

    private int executeByStrategy(Long productId, int quantity, ConcurrencyStrategy strategy) {
        switch (strategy) {
            case PESSIMISTIC_LOCK:
                return transactionManager.executePessimisticPurchase(productId, quantity);

            case OPTIMISTIC_LOCK:
                return transactionManager.executeOptimisticPurchase(productId, quantity);

            case ATOMIC_UPDATE:
                return transactionManager.executeAtomicUpdatePurchase(productId, quantity);

            case DISTRIBUTED_LOCK:
                return redisDistributedLockService.executeWithLock(
                        "product:" + productId,
                        3000,
                        5,
                        () -> transactionManager.executePessimisticPurchase(productId, quantity)
                );

            default:
                return transactionManager.executeAtomicUpdatePurchase(productId, quantity);
        }
    }
}
