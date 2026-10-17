package com.nexora.concurrency.controller;

import com.nexora.concurrency.dto.ConcurrencySimulationRequest;
import com.nexora.concurrency.dto.ConcurrencySimulationResult;
import com.nexora.concurrency.dto.ConcurrencyStrategyInfoResponse;
import com.nexora.concurrency.dto.FlashSalePurchaseRequest;
import com.nexora.concurrency.dto.FlashSalePurchaseResponse;
import com.nexora.concurrency.service.ConcurrencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Phase 21: Concurrency, Race Condition Prevention,
 * and Flash Sale Simulation Engine.
 */
@RestController
@RequestMapping("/api/v1/concurrency")
@RequiredArgsConstructor
public class ConcurrencyController {

    private final ConcurrencyService concurrencyService;

    /**
     * Run high-concurrency simulation benchmark (e.g. 100 simultaneous threads competing for 1 stock).
     */
    @PostMapping("/simulate")
    public ResponseEntity<ConcurrencySimulationResult> simulateFlashSale(
            @Valid @RequestBody ConcurrencySimulationRequest request
    ) {
        ConcurrencySimulationResult result = concurrencyService.simulateConcurrentFlashSale(request);
        return ResponseEntity.ok(result);
    }

    /**
     * Purchase a product during a flash sale with a specified locking strategy.
     */
    @PostMapping("/flash-sale/purchase")
    public ResponseEntity<FlashSalePurchaseResponse> purchaseFlashSale(
            @Valid @RequestBody FlashSalePurchaseRequest request
    ) {
        FlashSalePurchaseResponse response = concurrencyService.purchaseFlashSale(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get architectural comparison and explanation of all concurrency locking strategies.
     */
    @GetMapping("/strategies")
    public ResponseEntity<List<ConcurrencyStrategyInfoResponse>> getStrategyComparisons() {
        List<ConcurrencyStrategyInfoResponse> list = concurrencyService.getStrategyComparisons();
        return ResponseEntity.ok(list);
    }
}
