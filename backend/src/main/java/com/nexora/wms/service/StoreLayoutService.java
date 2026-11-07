package com.nexora.wms.service;

import com.nexora.exception.ResourceNotFoundException;
import com.nexora.product.entity.Product;
import com.nexora.product.repository.ProductRepository;
import com.nexora.store.entity.Store;
import com.nexora.store.repository.StoreRepository;
import com.nexora.wms.dto.StoreLayoutResponse;
import com.nexora.wms.entity.*;
import com.nexora.wms.enums.BinStatus;
import com.nexora.wms.enums.ZoneTemperatureType;
import com.nexora.wms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DARK STORE WMS LAYOUT & BIN LOCATION SERVICE (NEXORA 3.0 WS1)
 *
 * Manages the physical 5-tier warehouse spatial hierarchy:
 * Dark Store -> Zone -> Aisle -> Rack -> Shelf -> Bin -> Product Inventory.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StoreLayoutService {

    private final StoreRepository storeRepository;
    private final StoreZoneRepository zoneRepository;
    private final StoreAisleRepository aisleRepository;
    private final StoreRackRepository rackRepository;
    private final StoreShelfRepository shelfRepository;
    private final StoreBinRepository binRepository;
    private final ProductRepository productRepository;
    private final ProductStorageLocationRepository productLocationRepository;

    @Transactional(readOnly = true)
    public StoreLayoutResponse getStoreLayout(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with id: " + storeId));

        List<StoreZone> zones = zoneRepository.findByStoreIdAndIsActiveTrue(storeId);
        List<StoreBin> allBins = binRepository.findByStoreIdOrderByTraversalSequenceAsc(storeId);

        Map<Long, List<StoreBin>> binsByAisle = allBins.stream()
                .filter(b -> b.getShelf() != null && b.getShelf().getRack() != null && b.getShelf().getRack().getAisle() != null)
                .collect(Collectors.groupingBy(b -> b.getShelf().getRack().getAisle().getId()));

        List<StoreLayoutResponse.StoreZoneDto> zoneDtos = new ArrayList<>();
        for (StoreZone zone : zones) {
            List<StoreAisle> aisles = aisleRepository.findByZoneIdOrderByAisleNumberAsc(zone.getId());
            List<StoreLayoutResponse.StoreAisleDto> aisleDtos = new ArrayList<>();

            for (StoreAisle aisle : aisles) {
                List<StoreBin> aisleBins = binsByAisle.getOrDefault(aisle.getId(), Collections.emptyList());
                List<StoreLayoutResponse.StoreBinDto> binDtos = aisleBins.stream()
                        .map(b -> StoreLayoutResponse.StoreBinDto.builder()
                                .binId(b.getId())
                                .binCode(b.getBinCode())
                                .fullLocationCode(b.getFullLocationCode())
                                .traversalSequence(b.getTraversalSequence())
                                .xCoord(b.getXCoord())
                                .yCoord(b.getYCoord())
                                .capacityUnits(b.getCapacityUnits())
                                .status(b.getStatus() != null ? b.getStatus().name() : "ACTIVE")
                                .rackId(b.getShelf().getRack().getId())
                                .rackCode(b.getShelf().getRack().getRackCode())
                                .shelfId(b.getShelf().getId())
                                .shelfCode(b.getShelf().getShelfCode())
                                .shelfLevel(b.getShelf().getShelfLevel())
                                .build())
                        .collect(Collectors.toList());

                aisleDtos.add(StoreLayoutResponse.StoreAisleDto.builder()
                        .aisleId(aisle.getId())
                        .aisleCode(aisle.getAisleCode())
                        .aisleNumber(aisle.getAisleNumber())
                        .direction(aisle.getDirection() != null ? aisle.getDirection().name() : "BIDIRECTIONAL")
                        .isRestricted(aisle.getIsRestricted())
                        .widthMeters(aisle.getWidthMeters())
                        .lengthMeters(aisle.getLengthMeters())
                        .bins(binDtos)
                        .build());
            }

            zoneDtos.add(StoreLayoutResponse.StoreZoneDto.builder()
                    .zoneId(zone.getId())
                    .zoneCode(zone.getZoneCode())
                    .name(zone.getName())
                    .temperatureType(zone.getTemperatureType() != null ? zone.getTemperatureType().name() : "AMBIENT")
                    .aisles(aisleDtos)
                    .build());
        }

        return StoreLayoutResponse.builder()
                .storeId(store.getId())
                .storeName(store.getName())
                .zones(zoneDtos)
                .build();
    }

    @Transactional
    public ProductStorageLocation assignProductToBin(Long storeId, Long productId, Long binId, boolean isPrimary) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + storeId));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        StoreBin bin = binRepository.findById(binId)
                .orElseThrow(() -> new ResourceNotFoundException("Bin not found: " + binId));

        if (!bin.getStore().getId().equals(storeId)) {
            throw new IllegalArgumentException("Bin #" + binId + " does not belong to store #" + storeId);
        }

        if (isPrimary) {
            // Unset existing primary bins for this product in this store
            productLocationRepository.findFirstByProductIdAndStoreIdAndIsPrimaryTrue(productId, storeId)
                    .ifPresent(existingPrimary -> {
                        existingPrimary.setIsPrimary(false);
                        productLocationRepository.save(existingPrimary);
                    });
        }

        ProductStorageLocation mapping = productLocationRepository.findByProductIdAndStoreId(productId, storeId).stream()
                .filter(l -> l.getBin().getId().equals(binId))
                .findFirst()
                .orElseGet(() -> ProductStorageLocation.builder()
                        .product(product)
                        .bin(bin)
                        .store(store)
                        .maxCapacity(bin.getCapacityUnits())
                        .currentStock(0)
                        .build());

        mapping.setIsPrimary(isPrimary);
        mapping = productLocationRepository.save(mapping);
        log.info("Assigned Product #{} to Bin '{}' in Store #{} (Primary={})",
                productId, bin.getFullLocationCode(), storeId, isPrimary);
        return mapping;
    }

    @Transactional(readOnly = true)
    public Optional<StoreBin> getPrimaryBinForProduct(Long storeId, Long productId) {
        return productLocationRepository.findFirstByProductIdAndStoreIdAndIsPrimaryTrue(productId, storeId)
                .map(ProductStorageLocation::getBin);
    }
}
