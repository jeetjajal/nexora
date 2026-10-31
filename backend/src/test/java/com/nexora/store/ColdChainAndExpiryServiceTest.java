package com.nexora.store;

import com.nexora.product.entity.Product;
import com.nexora.store.dto.InventoryBatchResponse;
import com.nexora.store.dto.TemperatureTelemetryRequest;
import com.nexora.store.dto.TemperatureTelemetryResponse;
import com.nexora.store.entity.InventoryBatch;
import com.nexora.store.entity.Store;
import com.nexora.store.entity.TemperatureLog;
import com.nexora.store.repository.InventoryBatchRepository;
import com.nexora.store.repository.StoreRepository;
import com.nexora.store.repository.TemperatureLogRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 4 - ColdChainAndExpiryService FEFO & IoT Tests")
class ColdChainAndExpiryServiceTest {

    @Mock
    private InventoryBatchRepository batchRepository;

    @Mock
    private TemperatureLogRepository temperatureLogRepository;

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private ColdChainAndExpiryService coldChainService;

    private Store testStore;
    private Product testProduct;
    private InventoryBatch earlyBatch;
    private InventoryBatch laterBatch;

    @BeforeEach
    void setUp() {
        testStore = Store.builder().id(4L).name("SG Highway Dark Store Hub").build();
        testProduct = Product.builder().id(101L).name("Amul Fresh Taaza Milk 500ml").build();

        earlyBatch = InventoryBatch.builder()
                .id(1L)
                .product(testProduct)
                .store(testStore)
                .batchNumber("BATCH-AMUL-001")
                .expiryDate(LocalDate.now().plusDays(2))
                .initialQuantity(20)
                .availableQuantity(20)
                .reservedQuantity(0)
                .isQuarantined(false)
                .build();

        laterBatch = InventoryBatch.builder()
                .id(2L)
                .product(testProduct)
                .store(testStore)
                .batchNumber("BATCH-AMUL-002")
                .expiryDate(LocalDate.now().plusDays(10))
                .initialQuantity(30)
                .availableQuantity(30)
                .reservedQuantity(0)
                .isQuarantined(false)
                .build();
    }

    @Test
    @DisplayName("FEFO algorithm allocates stock from earliest expiring batch first")
    void testAllocateFefoStock() {
        when(batchRepository.findByProductIdAndStoreIdAndIsQuarantinedFalseAndAvailableQuantityGreaterThanOrderByExpiryDateAsc(101L, 4L, 0))
                .thenReturn(List.of(earlyBatch, laterBatch));

        // Request 25 units (20 should come from earlyBatch, 5 from laterBatch)
        List<InventoryBatch> allocated = coldChainService.allocateFefoStock(101L, 4L, 25);

        assertEquals(2, allocated.size());
        assertEquals(0, earlyBatch.getAvailableQuantity(), "Earliest batch should be completely consumed first");
        assertEquals(20, earlyBatch.getReservedQuantity());

        assertEquals(25, laterBatch.getAvailableQuantity(), "Remaining 5 units taken from later batch");
        assertEquals(5, laterBatch.getReservedQuantity());

        verify(batchRepository, times(2)).save(any(InventoryBatch.class));
    }

    @Test
    @DisplayName("Quarantined batch is blocked and excluded from customer picking")
    void testToggleQuarantine() {
        when(batchRepository.findById(1L)).thenReturn(Optional.of(earlyBatch));
        when(batchRepository.save(any(InventoryBatch.class))).thenReturn(earlyBatch);

        InventoryBatchResponse response = coldChainService.toggleQuarantine(1L, true);

        assertTrue(earlyBatch.getIsQuarantined());
        assertTrue(response.getIsQuarantined());
    }

    @Test
    @DisplayName("Temperature telemetry detects IoT breach above 8°C for Chiller")
    void testTemperatureBreachAlert() {
        when(storeRepository.findById(4L)).thenReturn(Optional.of(testStore));
        when(temperatureLogRepository.save(any(TemperatureLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TemperatureTelemetryRequest req = TemperatureTelemetryRequest.builder()
                .storeId(4L)
                .storageUnit("CHILLER_01")
                .temperatureCelsius(9.5) // Breach! (> 8.0°C)
                .humidityPercentage(65.0)
                .build();

        TemperatureTelemetryResponse response = coldChainService.recordTemperatureTelemetry(req);

        assertTrue(response.getBreachDetected());
        assertEquals("BREACH_ALERT", response.getStatus());
        assertEquals(9.5, response.getTemperatureCelsius());
    }

    @Test
    @DisplayName("Normal temperature within safe bounds returns NORMAL status")
    void testTemperatureNormalTelemetry() {
        when(storeRepository.findById(4L)).thenReturn(Optional.of(testStore));
        when(temperatureLogRepository.save(any(TemperatureLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TemperatureTelemetryRequest req = TemperatureTelemetryRequest.builder()
                .storeId(4L)
                .storageUnit("CHILLER_01")
                .temperatureCelsius(4.2) // Safe (2°C - 8°C)
                .humidityPercentage(60.0)
                .build();

        TemperatureTelemetryResponse response = coldChainService.recordTemperatureTelemetry(req);

        assertFalse(response.getBreachDetected());
        assertEquals("NORMAL", response.getStatus());
    }
}
