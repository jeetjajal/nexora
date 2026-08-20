package com.nexora.phase6;

import com.nexora.inventory.dto.StockAdjustmentRequest;
import com.nexora.inventory.entity.Inventory;
import com.nexora.inventory.exception.InsufficientStockException;
import com.nexora.inventory.repository.InventoryRepository;
import com.nexora.inventory.service.InventoryService;
import com.nexora.auth.security.UserPrincipal;
import com.nexora.product.entity.Product;
import com.nexora.product.repository.ProductRepository;
import com.nexora.role.entity.Role;
import com.nexora.role.entity.RoleName;
import com.nexora.role.repository.RoleRepository;
import com.nexora.store.entity.Store;
import com.nexora.store.entity.StoreStatus;
import com.nexora.store.repository.StoreRepository;
import com.nexora.user.entity.User;
import com.nexora.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * THIS IS THE TEST THAT ACTUALLY PROVES "never allow negative inventory"
 * — not just under a single sequential request, but under GENUINE
 * concurrent access from multiple threads at once, each running its
 * own real database transaction against the same Inventory row.
 *
 * THE SETUP: stock = 1, ten threads all fire "reduce stock by 1" AT
 * THE SAME MOMENT (synchronized to start together via CountDownLatch).
 * If InventoryService used a naive read-then-write approach, several
 * of these could all read quantity=1, all decide "yes, I can take
 * it," and all succeed — ending with negative stock. With the atomic
 * conditional UPDATE (InventoryRepository.reduceStockIfAvailable), the
 * database itself serializes access to that row: exactly ONE thread's
 * UPDATE can matter for the single unit of stock, and the other nine
 * cleanly receive InsufficientStockException.
 *
 * We call the real Spring-managed InventoryService bean (not a raw
 * `new InventoryService(...)`), so each thread's call goes through
 * Spring's real @Transactional proxy and opens its own genuine
 * database transaction — this is not a simulation.
 */
@SpringBootTest
@DisplayName("Phase 6: Inventory concurrency - never allow negative stock")
class InventoryConcurrencyIntegrationTest {

    @Autowired private InventoryService inventoryService;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User seedStoreOwner() {
        Role storeOwnerRole = roleRepository.findByName(RoleName.STORE_OWNER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.STORE_OWNER).build()));

        String unique = UUID.randomUUID().toString().substring(0, 8);
        return userRepository.save(User.builder()
                .name("Concurrency Test Owner")
                .email("concurrency." + unique + "@example.com")
                .password(passwordEncoder.encode("Password123"))
                .phone("9000000000")
                .status("ACTIVE")
                .roles(Set.of(storeOwnerRole))
                .build());
    }

    private Product seedProductWithStock(User owner, int startingStock) {
        Store store = storeRepository.save(Store.builder()
                .owner(owner)
                .name("Concurrency Test Store " + UUID.randomUUID().toString().substring(0, 8))
                .status(StoreStatus.OPEN)
                .build());

        Product product = productRepository.save(Product.builder()
                .store(store)
                .name("Last Item In Stock " + UUID.randomUUID().toString().substring(0, 8))
                .price(new BigDecimal("999.00"))
                .available(true)
                .build());

        inventoryRepository.save(Inventory.builder()
                .product(product)
                .quantity(startingStock)
                .lowStockThreshold(5)
                .build());

        return product;
    }

    @Test
    @DisplayName("With stock=1, exactly ONE of 10 simultaneous reduce-by-1 requests succeeds")
    void exactlyOneSimultaneousRequestSucceedsWhenStockIsOne() throws InterruptedException {
        User owner = seedStoreOwner();
        Product product = seedProductWithStock(owner, 1); // only one unit exists
        UserPrincipal principal = new UserPrincipal(owner);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger insufficientStockCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await(); // all threads wait here, then release together

                    inventoryService.reduceStock(product.getId(), new StockAdjustmentRequest(1), principal);
                    successCount.incrementAndGet();
                } catch (InsufficientStockException ex) {
                    insufficientStockCount.incrementAndGet();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS); // wait until every thread is queued up
        startLatch.countDown();                 // release all 10 threads at once
        boolean completed = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).as("all threads should finish within the timeout").isTrue();

        // THE CORE ASSERTION: exactly one thread wins the single unit
        // of stock; the other nine cleanly fail with "insufficient stock."
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(insufficientStockCount.get()).isEqualTo(threadCount - 1);

        // THE ULTIMATE PROOF: read the real, final, committed quantity
        // from the database — it must be exactly 0, never negative,
        // regardless of how many threads raced for it.
        Inventory finalState = inventoryRepository.findByProductId(product.getId()).orElseThrow();
        assertThat(finalState.getQuantity()).isEqualTo(0);
        assertThat(finalState.getQuantity()).isNotNegative();
    }

    @Test
    @DisplayName("With stock=5, exactly 5 of 20 simultaneous reduce-by-1 requests succeed")
    void exactlyStockCountSucceedsUnderHeavierConcurrency() throws InterruptedException {
        User owner = seedStoreOwner();
        Product product = seedProductWithStock(owner, 5);
        UserPrincipal principal = new UserPrincipal(owner);

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    inventoryService.reduceStock(product.getId(), new StockAdjustmentRequest(1), principal);
                    successCount.incrementAndGet();
                } catch (InsufficientStockException ex) {
                    failureCount.incrementAndGet();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(5);   // exactly as much stock as existed
        assertThat(failureCount.get()).isEqualTo(15);  // everyone else cleanly rejected

        Inventory finalState = inventoryRepository.findByProductId(product.getId()).orElseThrow();
        assertThat(finalState.getQuantity()).isEqualTo(0);
        assertThat(finalState.getQuantity()).isNotNegative();
    }

    @Test
    @DisplayName("Simultaneous increase operations are all reflected - no lost updates")
    void simultaneousIncreasesAreAllApplied() throws InterruptedException {
        User owner = seedStoreOwner();
        Product product = seedProductWithStock(owner, 0);
        UserPrincipal principal = new UserPrincipal(owner);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    inventoryService.increaseStock(product.getId(), new StockAdjustmentRequest(10), principal);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        // 10 threads x +10 each = +100 total, none lost to a race condition
        Inventory finalState = inventoryRepository.findByProductId(product.getId()).orElseThrow();
        assertThat(finalState.getQuantity()).isEqualTo(100);
    }
}
