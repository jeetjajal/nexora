package com.nexora.concurrency.service;

import com.nexora.concurrency.dto.ConcurrencySimulationRequest;
import com.nexora.concurrency.dto.ConcurrencySimulationResult;
import com.nexora.concurrency.dto.ConcurrencyStrategyInfoResponse;
import com.nexora.concurrency.dto.FlashSalePurchaseRequest;
import com.nexora.concurrency.dto.FlashSalePurchaseResponse;

import java.util.List;

/**
 * Service for handling advanced concurrency, race condition prevention,
 * and high-traffic flash sale simulations (Phase 21).
 */
public interface ConcurrencyService {

    /**
     * Attempt a single flash sale purchase using the chosen locking strategy.
     */
    FlashSalePurchaseResponse purchaseFlashSale(FlashSalePurchaseRequest request);

    /**
     * Execute a high-concurrency simulation (e.g. 100 simultaneous threads attempting to purchase stock=1).
     * Synchronizes all threads using CountDownLatch to guarantee true concurrent collision.
     */
    ConcurrencySimulationResult simulateConcurrentFlashSale(ConcurrencySimulationRequest request);

    /**
     * Provide architectural comparison and explanations of database transactions,
     * optimistic locking, pessimistic locking, and distributed locking.
     */
    List<ConcurrencyStrategyInfoResponse> getStrategyComparisons();
}
