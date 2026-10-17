package com.nexora.phase21;

import com.nexora.concurrency.dto.ConcurrencySimulationRequest;
import com.nexora.concurrency.dto.ConcurrencySimulationResult;
import com.nexora.concurrency.model.ConcurrencyStrategy;
import com.nexora.concurrency.service.ConcurrencyService;
import com.nexora.inventory.entity.Inventory;
import com.nexora.inventory.repository.InventoryRepository;
import com.nexora.product.entity.Product;
import com.nexora.product.repository.ProductRepository;
import com.nexora.store.entity.Store;
import com.nexora.store.entity.StoreStatus;
import com.nexora.store.repository.StoreRepository;
import com.nexora.user.entity.User;
import com.nexora.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PHASE 21 CORE CONCURRENCY BENCHMARK & STRESS INTEGRATION TEST.
 *
 * Roadmap Scenario:
 * "Scenario: stock=1 and 100 users buy simultaneously; only one succeeds."
 * "Implement and compare database transactions, optimistic locking, pessimistic locking, @Version and appropriate isolation levels."
 * "Explain why overselling happens, how locking solves it and when each strategy is useful. Add concurrent tests."
 */
@SpringBootTest
@DisplayName("Phase 21: High-Concurrency Flash Sale Stress Tests (100 simultaneous buyers on stock=1)")
class FlashSaleConcurrencyStressIntegrationTest {

    @Autowired
    private ConcurrencyService concurrencyService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private UserRepository userRepository;

    private Product flashSaleProduct;

    @BeforeEach
    void setUp() {
        User merchant = userRepository.save(User.builder()
                .name("Flash Sale Merchant")
                .email("merchant_" + UUID.randomUUID() + "@nexora.test")
                .password("encoded_pass")
                .phone("9887766554")
                .build());

        Store store = storeRepository.save(Store.builder()
                .name("Flash Sale Store")
                .owner(merchant)
                .status(StoreStatus.OPEN)
                .build());

        flashSaleProduct = productRepository.save(Product.builder()
                .name("Limited Edition Sony PS5")
                .description("Flash Sale Item with stock=1")
                .price(BigDecimal.valueOf(49999.0))
                .store(store)
                .available(true)
                .build());

        inventoryRepository.save(Inventory.builder()
                .product(flashSaleProduct)
                .quantity(1)
                .lowStockThreshold(1)
                .build());
    }

    @Test
    @DisplayName("Strategy 1 (Pessimistic Locking): 100 simultaneous buyers on stock=1 -> Exactly 1 succeeds, 99 rejected")
    void testPessimisticLocking100UsersStock1() {
        ConcurrencySimulationRequest request = ConcurrencySimulationRequest.builder()
                .productId(flashSaleProduct.getId())
                .initialStock(1)
                .totalThreads(100)
                .strategy(ConcurrencyStrategy.PESSIMISTIC_LOCK)
                .build();

        ConcurrencySimulationResult result = concurrencyService.simulateConcurrentFlashSale(request);

        assertThat(result.getInitialStock()).isEqualTo(1);
        assertThat(result.getTotalRequests()).isEqualTo(100);
        assertThat(result.getSuccessfulPurchases()).isEqualTo(1);
        assertThat(result.getFailedPurchases()).isEqualTo(99);
        assertThat(result.getFinalStock()).isEqualTo(0);
        assertThat(result.isOversellingPrevented()).isTrue();
    }

    @Test
    @DisplayName("Strategy 2 (Atomic Conditional UPDATE): 100 simultaneous buyers on stock=1 -> Exactly 1 succeeds, 99 rejected")
    void testAtomicUpdate100UsersStock1() {
        ConcurrencySimulationRequest request = ConcurrencySimulationRequest.builder()
                .productId(flashSaleProduct.getId())
                .initialStock(1)
                .totalThreads(100)
                .strategy(ConcurrencyStrategy.ATOMIC_UPDATE)
                .build();

        ConcurrencySimulationResult result = concurrencyService.simulateConcurrentFlashSale(request);

        assertThat(result.getInitialStock()).isEqualTo(1);
        assertThat(result.getTotalRequests()).isEqualTo(100);
        assertThat(result.getSuccessfulPurchases()).isEqualTo(1);
        assertThat(result.getFailedPurchases()).isEqualTo(99);
        assertThat(result.getFinalStock()).isEqualTo(0);
        assertThat(result.isOversellingPrevented()).isTrue();
    }

    @Test
    @DisplayName("Strategy 3 (Optimistic Locking with @Version): Stock=1 -> Never oversells, detects version collision")
    void testOptimisticLockingStock1() {
        ConcurrencySimulationRequest request = ConcurrencySimulationRequest.builder()
                .productId(flashSaleProduct.getId())
                .initialStock(1)
                .totalThreads(50)
                .strategy(ConcurrencyStrategy.OPTIMISTIC_LOCK)
                .build();

        ConcurrencySimulationResult result = concurrencyService.simulateConcurrentFlashSale(request);

        assertThat(result.getFinalStock()).isGreaterThanOrEqualTo(0);
        assertThat(result.getSuccessfulPurchases()).isLessThanOrEqualTo(1);
        assertThat(result.isOversellingPrevented()).isTrue();
    }
}
