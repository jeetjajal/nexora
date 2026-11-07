package com.nexora.wms;

import com.nexora.exception.ResourceNotFoundException;
import com.nexora.inventory.repository.InventoryRepository;
import com.nexora.product.entity.Product;
import com.nexora.product.repository.ProductRepository;
import com.nexora.store.entity.InventoryBatch;
import com.nexora.store.entity.Store;
import com.nexora.store.repository.InventoryBatchRepository;
import com.nexora.wms.dto.BarcodeResolveResponse;
import com.nexora.wms.entity.ProductBarcode;
import com.nexora.wms.entity.StoreBin;
import com.nexora.wms.enums.BarcodeType;
import com.nexora.wms.repository.PickingEventRepository;
import com.nexora.wms.repository.ProductBarcodeRepository;
import com.nexora.wms.service.BarcodeScanningService;
import com.nexora.wms.service.StoreLayoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 3: WS2 — Barcode & QR Resolution Engine Tests")
class BarcodeScanningServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductBarcodeRepository barcodeRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private InventoryBatchRepository batchRepository;
    @Mock private StoreLayoutService layoutService;
    @Mock private PickingEventRepository pickingEventRepository;

    @InjectMocks
    private BarcodeScanningService barcodeScanningService;

    private Store mockStore;
    private Product mockProduct;
    private ProductBarcode mockBarcode;
    private StoreBin mockBin;

    @BeforeEach
    void setUp() {
        mockStore = new Store();
        mockStore.setId(4L);
        mockStore.setName("Ahmedabad Dark Store");

        mockProduct = new Product();
        mockProduct.setId(101L);
        mockProduct.setName("Tata Salt Iodized 1kg");
        mockProduct.setSku("SKU-SALT-101");
        mockProduct.setPrimaryBarcode("8901030010015");
        mockProduct.setPrice(new BigDecimal("28.00"));

        mockBarcode = ProductBarcode.builder()
                .id(1L)
                .product(mockProduct)
                .barcodeValue("8901030010015")
                .barcodeType(BarcodeType.EAN_13)
                .isPrimary(true)
                .build();

        mockBin = StoreBin.builder()
                .id(201L)
                .fullLocationCode("ST04-ZAMB-A02-R01-S02-B03")
                .build();
    }

    @Test
    @DisplayName("WS2: Resolve product by direct barcode mapping")
    void testResolveProductByBarcode() {
        when(barcodeRepository.findByBarcodeValue("8901030010015")).thenReturn(Optional.of(mockBarcode));

        Optional<Product> resolved = barcodeScanningService.resolveProduct("8901030010015");

        assertTrue(resolved.isPresent());
        assertEquals(101L, resolved.get().getId());
        assertEquals("Tata Salt Iodized 1kg", resolved.get().getName());
    }

    @Test
    @DisplayName("WS2: Resolve product by SKU code")
    void testResolveProductBySku() {
        when(barcodeRepository.findByBarcodeValue("SKU-SALT-101")).thenReturn(Optional.empty());
        when(productRepository.findAll()).thenReturn(List.of(mockProduct));

        Optional<Product> resolved = barcodeScanningService.resolveProduct("SKU-SALT-101");

        assertTrue(resolved.isPresent());
        assertEquals("SKU-SALT-101", resolved.get().getSku());
    }

    @Test
    @DisplayName("WS2: Resolve product details with primary bin and non-expired batches")
    void testResolveBarcodeDetailsSuccess() {
        when(barcodeRepository.findByBarcodeValue("8901030010015")).thenReturn(Optional.of(mockBarcode));
        when(layoutService.getPrimaryBinForProduct(4L, 101L)).thenReturn(Optional.of(mockBin));

        InventoryBatch validBatch = InventoryBatch.builder()
                .id(301L)
                .product(mockProduct)
                .batchNumber("BATCH-2026-09")
                .expiryDate(LocalDate.now().plusMonths(6))
                .availableQuantity(25)
                .temperatureZone("AMBIENT")
                .isQuarantined(false)
                .build();

        InventoryBatch expiredBatch = InventoryBatch.builder()
                .id(302L)
                .product(mockProduct)
                .batchNumber("BATCH-2025-12")
                .expiryDate(LocalDate.now().minusDays(10))
                .availableQuantity(5)
                .temperatureZone("AMBIENT")
                .isQuarantined(false)
                .build();

        when(batchRepository.findByStoreIdOrderByExpiryDateAsc(4L))
                .thenReturn(List.of(validBatch, expiredBatch));

        BarcodeResolveResponse response = barcodeScanningService.resolveBarcodeDetails(4L, "8901030010015");

        assertNotNull(response);
        assertEquals(101L, response.getProductId());
        assertEquals("ST04-ZAMB-A02-R01-S02-B03", response.getPrimaryBinCode());
        // Expired batch must be filtered out!
        assertEquals(1, response.getAvailableBatches().size());
        assertEquals("BATCH-2026-09", response.getAvailableBatches().get(0).getBatchNumber());
    }

    @Test
    @DisplayName("WS2: Unknown barcode throws ResourceNotFoundException")
    void testResolveBarcodeNotFound() {
        when(barcodeRepository.findByBarcodeValue("UNKNOWN_CODE_999")).thenReturn(Optional.empty());
        when(productRepository.findAll()).thenReturn(List.of(mockProduct));

        assertThrows(ResourceNotFoundException.class, () ->
                barcodeScanningService.resolveBarcodeDetails(4L, "UNKNOWN_CODE_999"));
    }
}
