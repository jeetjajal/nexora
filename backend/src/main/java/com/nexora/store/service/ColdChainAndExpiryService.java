package com.nexora.store.service;

import com.nexora.exception.ResourceNotFoundException;
import com.nexora.product.entity.Product;
import com.nexora.product.repository.ProductRepository;
import com.nexora.store.dto.*;
import com.nexora.store.entity.InventoryBatch;
import com.nexora.store.entity.Store;
import com.nexora.store.entity.TemperatureLog;
import com.nexora.store.repository.InventoryBatchRepository;
import com.nexora.store.repository.StoreRepository;
import com.nexora.store.repository.TemperatureLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ColdChainAndExpiryService {

    private final InventoryBatchRepository batchRepository;
    private final TemperatureLogRepository temperatureLogRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;

    private static final Map<String, double[]> UNIT_SAFE_RANGES = Map.of(
            "CHILLER_01", new double[]{2.0, 8.0},
            "CHILLER_02", new double[]{2.0, 8.0},
            "FREEZER_01", new double[]{-24.0, -15.0},
            "FREEZER_02", new double[]{-24.0, -15.0},
            "TRANSIT_COOLBOX_12", new double[]{2.0, 10.0}
    );

    @Transactional(readOnly = true)
    public List<InventoryBatchResponse> getStoreBatches(Long storeId) {
        return batchRepository.findByStoreIdOrderByExpiryDateAsc(storeId).stream()
                .map(this::mapToBatchResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public InventoryBatchResponse createBatch(CreateInventoryBatchRequest req) {
        Product product = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + req.getProductId()));

        Store store = storeRepository.findById(req.getStoreId())
                .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + req.getStoreId()));

        InventoryBatch batch = InventoryBatch.builder()
                .product(product)
                .store(store)
                .batchNumber(req.getBatchNumber())
                .mfgDate(req.getMfgDate() != null ? req.getMfgDate() : LocalDate.now().minusDays(2))
                .expiryDate(req.getExpiryDate())
                .initialQuantity(req.getQuantity())
                .availableQuantity(req.getQuantity())
                .reservedQuantity(0)
                .unitCost(req.getUnitCost() != null ? req.getUnitCost() : BigDecimal.ZERO)
                .temperatureZone(req.getTemperatureZone() != null ? req.getTemperatureZone() : "AMBIENT")
                .isQuarantined(false)
                .discountAppliedPercent(0)
                .build();

        batch = batchRepository.save(batch);
        evaluateDiscount(batch);
        return mapToBatchResponse(batch);
    }

    /**
     * First-Expiry-First-Out (FEFO) Allocation Engine:
     * Allocates stock strictly from non-quarantined batches expiring earliest.
     */
    @Transactional
    public List<InventoryBatch> allocateFefoStock(Long productId, Long storeId, int requestedQty) {
        List<InventoryBatch> eligibleBatches = batchRepository
                .findByProductIdAndStoreIdAndIsQuarantinedFalseAndAvailableQuantityGreaterThanOrderByExpiryDateAsc(
                        productId, storeId, 0
                );

        int remaining = requestedQty;
        List<InventoryBatch> allocatedBatches = new ArrayList<>();

        for (InventoryBatch batch : eligibleBatches) {
            if (remaining <= 0) break;

            int toTake = Math.min(batch.getAvailableQuantity(), remaining);
            batch.setAvailableQuantity(batch.getAvailableQuantity() - toTake);
            batch.setReservedQuantity(batch.getReservedQuantity() + toTake);
            remaining -= toTake;
            batchRepository.save(batch);
            allocatedBatches.add(batch);
            log.info("FEFO Allocated {} units of SKU #{} from batch #{} (Expires: {})",
                    toTake, productId, batch.getBatchNumber(), batch.getExpiryDate());
        }

        if (remaining > 0) {
            log.warn("FEFO short-allocation for SKU #{}: needed {}, fulfilled {}",
                    productId, requestedQty, requestedQty - remaining);
        }

        return allocatedBatches;
    }

    @Transactional
    public InventoryBatchResponse toggleQuarantine(Long batchId, boolean quarantined) {
        InventoryBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory batch not found: " + batchId));

        batch.setIsQuarantined(quarantined);
        batch = batchRepository.save(batch);
        log.info("Batch #{} quarantine status updated to {}", batch.getBatchNumber(), quarantined);
        return mapToBatchResponse(batch);
    }

    @Transactional
    public TemperatureTelemetryResponse recordTemperatureTelemetry(TemperatureTelemetryRequest req) {
        Store store = storeRepository.findById(req.getStoreId())
                .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + req.getStoreId()));

        String unit = req.getStorageUnit();
        double[] range = UNIT_SAFE_RANGES.getOrDefault(unit, new double[]{2.0, 8.0});
        double minSafe = range[0];
        double maxSafe = range[1];

        boolean breach = (req.getTemperatureCelsius() < minSafe || req.getTemperatureCelsius() > maxSafe);

        TemperatureLog logEntry = TemperatureLog.builder()
                .store(store)
                .storageUnit(unit)
                .temperatureCelsius(req.getTemperatureCelsius())
                .humidityPercentage(req.getHumidityPercentage())
                .minSafeTemp(minSafe)
                .maxSafeTemp(maxSafe)
                .breachDetected(breach)
                .recordedAt(LocalDateTime.now())
                .build();

        logEntry = temperatureLogRepository.save(logEntry);

        if (breach) {
            log.error("CRITICAL COLD CHAIN BREACH at Store #{}, Unit {}: Temp {}°C outside safe [{}, {}]°C",
                    store.getId(), unit, req.getTemperatureCelsius(), minSafe, maxSafe);
        }

        return TemperatureTelemetryResponse.builder()
                .id(logEntry.getId())
                .storeId(store.getId())
                .storageUnit(unit)
                .temperatureCelsius(req.getTemperatureCelsius())
                .humidityPercentage(req.getHumidityPercentage())
                .minSafeTemp(minSafe)
                .maxSafeTemp(maxSafe)
                .breachDetected(breach)
                .status(breach ? "BREACH_ALERT" : "NORMAL")
                .recordedAt(logEntry.getRecordedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public ColdChainOverviewResponse getColdChainOverview(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + storeId));

        List<InventoryBatch> batches = batchRepository.findByStoreIdOrderByExpiryDateAsc(storeId);
        int totalBatches = batches.size();
        int quarantined = (int) batches.stream().filter(InventoryBatch::getIsQuarantined).count();

        LocalDate today = LocalDate.now();
        int nearExpiry = (int) batches.stream()
                .filter(b -> !b.getIsQuarantined() && ChronoUnit.DAYS.between(today, b.getExpiryDate()) <= 2 && ChronoUnit.DAYS.between(today, b.getExpiryDate()) > 0)
                .count();

        List<ColdChainOverviewResponse.StorageUnitHealthDto> units = List.of(
                buildUnitHealth("CHILLER_01", 4.2, 4.0, 2.0, 8.0, false),
                buildUnitHealth("CHILLER_02", 3.8, 4.0, 2.0, 8.0, false),
                buildUnitHealth("FREEZER_01", -18.5, -18.0, -24.0, -15.0, false),
                buildUnitHealth("TRANSIT_COOLBOX_12", 5.1, 4.0, 2.0, 10.0, false)
        );

        return ColdChainOverviewResponse.builder()
                .storeId(store.getId())
                .storeName(store.getName())
                .totalBatches(totalBatches)
                .activeBatches(totalBatches - quarantined)
                .nearExpiryCount(nearExpiry)
                .quarantinedCount(quarantined)
                .units(units)
                .build();
    }

    private ColdChainOverviewResponse.StorageUnitHealthDto buildUnitHealth(
            String name, double curr, double target, double min, double max, boolean breach) {
        return ColdChainOverviewResponse.StorageUnitHealthDto.builder()
                .storageUnit(name)
                .currentTemp(curr)
                .targetTemp(target)
                .minSafe(min)
                .maxSafe(max)
                .inBreach(breach)
                .lastUpdated("Just now")
                .build();
    }

    private void evaluateDiscount(InventoryBatch batch) {
        long days = ChronoUnit.DAYS.between(LocalDate.now(), batch.getExpiryDate());
        if (days <= 0) {
            batch.setIsQuarantined(true);
            batch.setDiscountAppliedPercent(0);
        } else if (days == 1) {
            batch.setDiscountAppliedPercent(40);
        } else if (days == 2) {
            batch.setDiscountAppliedPercent(20);
        } else {
            batch.setDiscountAppliedPercent(0);
        }
        batchRepository.save(batch);
    }

    private InventoryBatchResponse mapToBatchResponse(InventoryBatch b) {
        long days = ChronoUnit.DAYS.between(LocalDate.now(), b.getExpiryDate());
        boolean isExpired = days <= 0;
        String status = isExpired ? "EXPIRED" : (days == 1 ? "CRITICAL_24H" : (days == 2 ? "NEAR_EXPIRY_48H" : "FRESH"));

        return InventoryBatchResponse.builder()
                .id(b.getId())
                .productId(b.getProduct().getId())
                .productName(b.getProduct().getName())
                .storeId(b.getStore().getId())
                .storeName(b.getStore().getName())
                .batchNumber(b.getBatchNumber())
                .mfgDate(b.getMfgDate())
                .expiryDate(b.getExpiryDate())
                .daysUntilExpiry(days)
                .initialQuantity(b.getInitialQuantity())
                .availableQuantity(b.getAvailableQuantity())
                .reservedQuantity(b.getReservedQuantity())
                .unitCost(b.getUnitCost())
                .temperatureZone(b.getTemperatureZone())
                .isQuarantined(b.getIsQuarantined())
                .discountAppliedPercent(b.getDiscountAppliedPercent())
                .isExpired(isExpired)
                .freshnessStatus(status)
                .build();
    }
}
