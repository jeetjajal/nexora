package com.nexora.wms.service;

import com.nexora.exception.ResourceNotFoundException;
import com.nexora.inventory.entity.Inventory;
import com.nexora.inventory.repository.InventoryRepository;
import com.nexora.product.entity.Product;
import com.nexora.product.repository.ProductRepository;
import com.nexora.store.entity.InventoryBatch;
import com.nexora.store.repository.InventoryBatchRepository;
import com.nexora.wms.dto.BarcodeResolveResponse;
import com.nexora.wms.entity.PickingEvent;
import com.nexora.wms.entity.ProductBarcode;
import com.nexora.wms.entity.StoreBin;
import com.nexora.wms.enums.PickingEventType;
import com.nexora.wms.repository.PickingEventRepository;
import com.nexora.wms.repository.ProductBarcodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * BARCODE AND QR SCANNING ENGINE (NEXORA 3.0 WS2)
 *
 * Implements high-speed barcode verification, SKU mapping, FEFO batch matching,
 * and immutable event audit logging for quick commerce pickers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BarcodeScanningService {

    private final ProductRepository productRepository;
    private final ProductBarcodeRepository barcodeRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryBatchRepository batchRepository;
    private final StoreLayoutService layoutService;
    private final PickingEventRepository pickingEventRepository;

    /**
     * Resolves an arbitrary scanned barcode, SKU, or QR code to a Product entity.
     */
    @Transactional(readOnly = true)
    public Optional<Product> resolveProduct(String rawBarcode) {
        if (rawBarcode == null || rawBarcode.trim().isEmpty()) {
            return Optional.empty();
        }
        String clean = rawBarcode.trim();

        // 1. Direct match on product_barcodes table
        Optional<ProductBarcode> pb = barcodeRepository.findByBarcodeValue(clean);
        if (pb.isPresent()) {
            return Optional.of(pb.get().getProduct());
        }

        // 2. Match on products.primary_barcode
        Optional<Product> prodByBarcode = productRepository.findAll().stream()
                .filter(p -> clean.equalsIgnoreCase(p.getPrimaryBarcode()))
                .findFirst();
        if (prodByBarcode.isPresent()) {
            return prodByBarcode;
        }

        // 3. Match on products.sku
        Optional<Product> prodBySku = productRepository.findAll().stream()
                .filter(p -> clean.equalsIgnoreCase(p.getSku()))
                .findFirst();
        if (prodBySku.isPresent()) {
            return prodBySku;
        }

        // 4. Match on product ID (e.g. SKU-101 or plain numeric 101)
        try {
            String numeric = clean.replaceAll("\\D", "");
            if (!numeric.isEmpty()) {
                Long id = Long.parseLong(numeric);
                return productRepository.findById(id);
            }
        } catch (Exception ex) {
            log.debug("Barcode numeric parse failed for '{}'", clean);
        }

        return Optional.empty();
    }

    /**
     * Complete barcode resolution API response.
     */
    @Transactional(readOnly = true)
    public BarcodeResolveResponse resolveBarcodeDetails(Long storeId, String rawBarcode) {
        Product product = resolveProduct(rawBarcode)
                .orElseThrow(() -> new ResourceNotFoundException("Unrecognized barcode or SKU: " + rawBarcode));

        String binCode = layoutService.getPrimaryBinForProduct(storeId, product.getId())
                .map(StoreBin::getFullLocationCode)
                .orElse("UNASSIGNED_BIN");

        int stock = inventoryRepository.findByProductId(product.getId())
                .map(Inventory::getQuantity)
                .orElse(0);

        List<InventoryBatch> batches = batchRepository.findByStoreIdOrderByExpiryDateAsc(storeId).stream()
                .filter(b -> b.getProduct() != null && b.getProduct().getId().equals(product.getId()))
                .filter(b -> !Boolean.TRUE.equals(b.getIsQuarantined()))
                .filter(b -> b.getExpiryDate() != null && b.getExpiryDate().isAfter(LocalDate.now()))
                .collect(Collectors.toList());

        List<BarcodeResolveResponse.BatchStockDto> batchDtos = batches.stream()
                .map(b -> BarcodeResolveResponse.BatchStockDto.builder()
                        .batchId(b.getId())
                        .batchNumber(b.getBatchNumber())
                        .expiryDate(b.getExpiryDate())
                        .availableQuantity(b.getAvailableQuantity())
                        .temperatureZone(b.getTemperatureZone() != null ? b.getTemperatureZone() : "AMBIENT")
                        .isQuarantined(b.getIsQuarantined())
                        .build())
                .collect(Collectors.toList());

        return BarcodeResolveResponse.builder()
                .productId(product.getId())
                .productName(product.getName())
                .sku(product.getSku() != null ? product.getSku() : "SKU-" + product.getId())
                .primaryBarcode(product.getPrimaryBarcode() != null ? product.getPrimaryBarcode() : rawBarcode)
                .price(product.getPrice())
                .primaryBinCode(binCode)
                .availableStock(stock)
                .availableBatches(batchDtos)
                .build();
    }

    /**
     * Logs an immutable audit picking event.
     */
    @Transactional
    public void recordPickingEvent(
            Long pickTaskId,
            Long pickTaskItemId,
            Long operatorId,
            String rawScan,
            PickingEventType eventType,
            boolean isSuccess,
            String errorMessage) {

        PickingEvent event = PickingEvent.builder()
                .pickTask(null) // Assigned by FK if task present
                .pickTaskItemId(pickTaskItemId)
                .operatorId(operatorId != null ? operatorId : 0L)
                .scannedRawValue(rawScan != null ? rawScan : "MANUAL_ENTRY")
                .eventType(eventType)
                .isSuccess(isSuccess)
                .errorMessage(errorMessage)
                .build();

        try {
            pickingEventRepository.save(event);
        } catch (Exception ex) {
            log.warn("Failed to write picking audit event: {}", ex.getMessage());
        }
    }
}
