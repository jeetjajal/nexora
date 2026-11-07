package com.nexora.wms;

import com.nexora.product.entity.Product;
import com.nexora.product.repository.ProductRepository;
import com.nexora.store.entity.Store;
import com.nexora.store.repository.StoreRepository;
import com.nexora.wms.dto.StoreLayoutResponse;
import com.nexora.wms.entity.*;
import com.nexora.wms.enums.BinStatus;
import com.nexora.wms.enums.ZoneTemperatureType;
import com.nexora.wms.repository.*;
import com.nexora.wms.service.StoreLayoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 3: WS1 — Dark Store Layout & Bin Location Management Tests")
class StoreLayoutAndBinMappingTest {

    @Mock private StoreRepository storeRepository;
    @Mock private StoreZoneRepository zoneRepository;
    @Mock private StoreAisleRepository aisleRepository;
    @Mock private StoreRackRepository rackRepository;
    @Mock private StoreShelfRepository shelfRepository;
    @Mock private StoreBinRepository binRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductStorageLocationRepository productLocationRepository;

    @InjectMocks
    private StoreLayoutService storeLayoutService;

    private Store mockStore;
    private StoreZone ambientZone;
    private StoreAisle aisle1;
    private StoreRack rack1;
    private StoreShelf shelf1;
    private StoreBin bin1;
    private StoreBin bin2;
    private Product mockProduct;

    @BeforeEach
    void setUp() {
        mockStore = new Store();
        mockStore.setId(4L);
        mockStore.setName("Ahmedabad SG Highway Dark Store");

        ambientZone = StoreZone.builder()
                .id(1L)
                .store(mockStore)
                .zoneCode("ZAMB")
                .name("Ambient Dry Goods")
                .temperatureType(ZoneTemperatureType.AMBIENT)
                .isActive(true)
                .build();

        aisle1 = StoreAisle.builder()
                .id(10L)
                .zone(ambientZone)
                .aisleCode("A01")
                .aisleNumber(1)
                .widthMeters(new BigDecimal("2.20"))
                .lengthMeters(new BigDecimal("15.00"))
                .isRestricted(false)
                .build();

        rack1 = StoreRack.builder()
                .id(20L)
                .aisle(aisle1)
                .rackCode("R01")
                .rackNumber(1)
                .build();

        shelf1 = StoreShelf.builder()
                .id(30L)
                .rack(rack1)
                .shelfCode("S01")
                .shelfLevel(1)
                .build();

        bin1 = StoreBin.builder()
                .id(100L)
                .store(mockStore)
                .shelf(shelf1)
                .binCode("B01")
                .fullLocationCode("ST04-ZAMB-A01-R01-S01-B01")
                .traversalSequence(1)
                .xCoord(new BigDecimal("2.00"))
                .yCoord(new BigDecimal("1.50"))
                .capacityUnits(50)
                .status(BinStatus.ACTIVE)
                .build();

        bin2 = StoreBin.builder()
                .id(101L)
                .store(mockStore)
                .shelf(shelf1)
                .binCode("B02")
                .fullLocationCode("ST04-ZAMB-A01-R01-S01-B02")
                .traversalSequence(2)
                .xCoord(new BigDecimal("2.00"))
                .yCoord(new BigDecimal("3.00"))
                .capacityUnits(50)
                .status(BinStatus.ACTIVE)
                .build();

        mockProduct = new Product();
        mockProduct.setId(501L);
        mockProduct.setName("Amul Taaza Toned Milk 1L");
        mockProduct.setSku("SKU-MILK-01");
        mockProduct.setPrimaryBarcode("8901262010014");
    }

    @Test
    @DisplayName("WS1: Successfully retrieve full 5-tier store layout")
    void testGetStoreLayoutSuccess() {
        when(storeRepository.findById(4L)).thenReturn(Optional.of(mockStore));
        when(zoneRepository.findByStoreIdAndIsActiveTrue(4L)).thenReturn(List.of(ambientZone));
        when(binRepository.findByStoreIdOrderByTraversalSequenceAsc(4L)).thenReturn(List.of(bin1, bin2));
        when(aisleRepository.findByZoneIdOrderByAisleNumberAsc(ambientZone.getId())).thenReturn(List.of(aisle1));

        StoreLayoutResponse response = storeLayoutService.getStoreLayout(4L);

        assertNotNull(response);
        assertEquals(4L, response.getStoreId());
        assertEquals("Ahmedabad SG Highway Dark Store", response.getStoreName());
        assertEquals(1, response.getZones().size());

        StoreLayoutResponse.StoreZoneDto zoneDto = response.getZones().get(0);
        assertEquals("ZAMB", zoneDto.getZoneCode());
        assertEquals(1, zoneDto.getAisles().size());

        StoreLayoutResponse.StoreAisleDto aisleDto = zoneDto.getAisles().get(0);
        assertEquals("A01", aisleDto.getAisleCode());
        assertEquals(2, aisleDto.getBins().size());
        assertEquals("ST04-ZAMB-A01-R01-S01-B01", aisleDto.getBins().get(0).getFullLocationCode());
    }

    @Test
    @DisplayName("WS1: Assign product to bin as primary location and verify old primary unset")
    void testAssignProductToPrimaryBin() {
        when(storeRepository.findById(4L)).thenReturn(Optional.of(mockStore));
        when(productRepository.findById(501L)).thenReturn(Optional.of(mockProduct));
        when(binRepository.findById(100L)).thenReturn(Optional.of(bin1));

        ProductStorageLocation oldPrimary = ProductStorageLocation.builder()
                .id(901L)
                .product(mockProduct)
                .store(mockStore)
                .bin(bin2)
                .isPrimary(true)
                .build();

        when(productLocationRepository.findFirstByProductIdAndStoreIdAndIsPrimaryTrue(501L, 4L))
                .thenReturn(Optional.of(oldPrimary));

        when(productLocationRepository.findByProductIdAndStoreId(501L, 4L))
                .thenReturn(List.of(oldPrimary));

        when(productLocationRepository.save(any(ProductStorageLocation.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ProductStorageLocation newMapping = storeLayoutService.assignProductToBin(4L, 501L, 100L, true);

        assertNotNull(newMapping);
        assertTrue(newMapping.getIsPrimary());
        assertEquals(bin1.getId(), newMapping.getBin().getId());

        // Verify old primary location was marked as non-primary
        assertFalse(oldPrimary.getIsPrimary());
        verify(productLocationRepository, atLeastOnce()).save(oldPrimary);
    }
}
