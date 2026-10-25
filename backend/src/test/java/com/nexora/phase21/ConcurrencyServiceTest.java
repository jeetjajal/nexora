package com.nexora.phase21;

import com.nexora.concurrency.dto.ConcurrencyStrategyInfoResponse;
import com.nexora.concurrency.dto.FlashSalePurchaseRequest;
import com.nexora.concurrency.dto.FlashSalePurchaseResponse;
import com.nexora.concurrency.model.ConcurrencyStrategy;
import com.nexora.concurrency.service.ConcurrencyServiceImpl;
import com.nexora.concurrency.service.ConcurrencyTransactionManager;
import com.nexora.inventory.entity.Inventory;
import com.nexora.inventory.exception.InsufficientStockException;
import com.nexora.inventory.repository.InventoryRepository;
import com.nexora.redis.service.RedisDistributedLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 21: ConcurrencyService Unit Tests")
class ConcurrencyServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ConcurrencyTransactionManager transactionManager;

    @Mock
    private RedisDistributedLockService redisDistributedLockService;

    @InjectMocks
    private ConcurrencyServiceImpl concurrencyService;

    @Test
    @DisplayName("Pessimistic purchase succeeds and returns remaining stock")
    void testPurchaseFlashSalePessimisticSuccess() {
        when(transactionManager.executePessimisticPurchase(1L, 1)).thenReturn(0);

        FlashSalePurchaseRequest request = FlashSalePurchaseRequest.builder()
                .productId(1L)
                .quantity(1)
                .strategy(ConcurrencyStrategy.PESSIMISTIC_LOCK)
                .build();

        FlashSalePurchaseResponse response = concurrencyService.purchaseFlashSale(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(0, response.getRemainingStock());
        assertEquals(ConcurrencyStrategy.PESSIMISTIC_LOCK, response.getStrategyUsed());
        verify(transactionManager).executePessimisticPurchase(1L, 1);
    }

    @Test
    @DisplayName("Flash sale purchase fails gracefully when stock is insufficient")
    void testPurchaseFlashSaleInsufficientStock() {
        when(transactionManager.executePessimisticPurchase(1L, 1))
                .thenThrow(new InsufficientStockException(1L, 1, 0));
        when(inventoryRepository.findByProductId(1L))
                .thenReturn(Optional.of(Inventory.builder().id(10L).quantity(0).build()));

        FlashSalePurchaseRequest request = FlashSalePurchaseRequest.builder()
                .productId(1L)
                .quantity(1)
                .strategy(ConcurrencyStrategy.PESSIMISTIC_LOCK)
                .build();

        FlashSalePurchaseResponse response = concurrencyService.purchaseFlashSale(request);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(0, response.getRemainingStock());
        assertTrue(response.getMessage().contains("Insufficient stock"));
    }

    @Test
    @DisplayName("getStrategyComparisons returns all 4 architectural strategies with descriptions")
    void testGetStrategyComparisons() {
        List<ConcurrencyStrategyInfoResponse> strategies = concurrencyService.getStrategyComparisons();

        assertNotNull(strategies);
        assertEquals(4, strategies.size());
        assertTrue(strategies.stream().anyMatch(s -> s.getStrategy() == ConcurrencyStrategy.PESSIMISTIC_LOCK));
        assertTrue(strategies.stream().anyMatch(s -> s.getStrategy() == ConcurrencyStrategy.OPTIMISTIC_LOCK));
        assertTrue(strategies.stream().anyMatch(s -> s.getStrategy() == ConcurrencyStrategy.ATOMIC_UPDATE));
        assertTrue(strategies.stream().anyMatch(s -> s.getStrategy() == ConcurrencyStrategy.DISTRIBUTED_LOCK));
    }
}
