package com.nexora.phase11;

import com.nexora.address.entity.Address;
import com.nexora.address.repository.AddressRepository;
import com.nexora.auth.security.UserPrincipal;
import com.nexora.cart.entity.Cart;
import com.nexora.cart.entity.CartItem;
import com.nexora.cart.repository.CartItemRepository;
import com.nexora.cart.repository.CartRepository;
import com.nexora.idempotency.exception.IdempotencyConflictException;
import com.nexora.inventory.entity.Inventory;
import com.nexora.inventory.repository.InventoryRepository;
import com.nexora.order.dto.CheckoutRequest;
import com.nexora.order.repository.OrderRepository;
import com.nexora.order.service.OrderService;
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
import org.springframework.data.domain.Pageable;
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
 * THIS IS THE TEST THAT PROVES the "network fails, client retries"
 * scenario is actually safe under GENUINE concurrency, not just
 * sequential retries — mirroring exactly the rigor
 * InventoryConcurrencyIntegrationTest (Phase 6) applied to stock
 * reduction. Real threads, the real Spring-managed OrderService bean,
 * real database transactions, all racing to check out with the exact
 * SAME Idempotency-Key at the exact same moment.
 *
 * If the unique constraint on (key_value, scope, user_id) didn't work,
 * multiple threads could all see "no existing record" at once and all
 * proceed to run checkout() — creating multiple orders and reducing
 * stock multiple times for what the client believes is ONE checkout
 * attempt. With it, at most one thread's checkout logic ever actually
 * runs; every other thread either gets rejected with a 409 (if it
 * arrives while the first is still mid-flight) or gets the identical
 * cached order back (if it arrives after the first already finished).
 * Either outcome is safe — what must never happen is a SECOND order.
 */
@SpringBootTest
@DisplayName("Phase 11: Idempotency concurrency - no duplicate checkout under simultaneous retries")
class IdempotencyConcurrencyIntegrationTest {

    @Autowired private OrderService orderService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private AddressRepository addressRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private CartRepository cartRepository;
    @Autowired private CartItemRepository cartItemRepository;

    private User seedCustomer() {
        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.CUSTOMER).build()));

        String unique = UUID.randomUUID().toString().substring(0, 8);
        return userRepository.save(User.builder()
                .name("Idempotency Concurrency Customer")
                .email("idem-concurrency." + unique + "@example.com")
                .password(passwordEncoder.encode("Password123"))
                .phone("9000000000")
                .status("ACTIVE")
                .roles(Set.of(customerRole))
                .build());
    }

    private User seedStoreOwner() {
        Role storeOwnerRole = roleRepository.findByName(RoleName.STORE_OWNER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.STORE_OWNER).build()));

        String unique = UUID.randomUUID().toString().substring(0, 8);
        return userRepository.save(User.builder()
                .name("Idempotency Concurrency Owner")
                .email("idem-owner." + unique + "@example.com")
                .password(passwordEncoder.encode("Password123"))
                .phone("9000000000")
                .status("ACTIVE")
                .roles(Set.of(storeOwnerRole))
                .build());
    }

    @Test
    @DisplayName("10 simultaneous checkout requests with the SAME Idempotency-Key produce exactly ONE order")
    void simultaneousCheckoutsWithSameKeyProduceOneOrder() throws InterruptedException {
        User owner = seedStoreOwner();
        User customer = seedCustomer();
        UserPrincipal principal = new UserPrincipal(customer);

        Store store = storeRepository.save(Store.builder()
                .owner(owner).name("Idem Concurrency Store").status(StoreStatus.OPEN).build());

        Product product = productRepository.save(Product.builder()
                .store(store).name("Idem Concurrency Product")
                .price(new BigDecimal("99.00")).available(true).build());

        inventoryRepository.save(Inventory.builder().product(product).quantity(50).build());

        Address address = addressRepository.save(Address.builder()
                .user(customer).addressLine1("1 Concurrency Street")
                .city("Rajkot").state("Gujarat").pincode("360001").build());

        // ONE cart, seeded ONCE — matching the real-world scenario the
        // roadmap describes: the client has ONE logical checkout
        // attempt in flight, retried N times because it doesn't know
        // whether an earlier attempt already succeeded. This test is
        // specifically isolating the idempotency guarantee itself (not
        // cart-emptying races, which are a separate, already-covered
        // concern) — see PaymentReliabilityIntegrationTest for the
        // full HTTP-level sequential-retry flow through Cart -> Checkout.
        Cart cart = cartRepository.save(Cart.builder().user(customer).build());
        cartItemRepository.save(CartItem.builder().cart(cart).product(product).quantity(1).build());

        String sharedIdempotencyKey = UUID.randomUUID().toString();
        CheckoutRequest request = new CheckoutRequest(address.getId());

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger otherErrorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    orderService.checkout(principal, request, sharedIdempotencyKey);
                    successCount.incrementAndGet();
                } catch (IdempotencyConflictException ex) {
                    conflictCount.incrementAndGet();
                } catch (Exception ex) {
                    otherErrorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();
        boolean completed = doneLatch.await(20, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).as("all threads should finish within the timeout").isTrue();

        // THE CORE ASSERTION: regardless of how the 10 threads split
        // between "succeeded" (real or replayed) and "409 conflict",
        // the real checkout logic (which creates an Order and reduces
        // stock) must have run AT MOST ONCE.
        long totalOrdersForThisCustomer =
                orderRepository.findByUserId(customer.getId(), Pageable.unpaged()).getTotalElements();
        assertThat(totalOrdersForThisCustomer).isEqualTo(1);

        Inventory finalInventory = inventoryRepository.findByProductId(product.getId()).orElseThrow();
        assertThat(finalInventory.getQuantity()).isEqualTo(49); // reduced exactly once, from 50

        // Every thread got a definitive outcome — no thread hung or
        // silently swallowed an unexpected error.
        assertThat(successCount.get() + conflictCount.get() + otherErrorCount.get()).isEqualTo(threadCount);
        assertThat(otherErrorCount.get())
                .as("no thread should hit an unexpected error type — only success or IdempotencyConflictException")
                .isEqualTo(0);
        assertThat(successCount.get())
                .as("at least one thread must have gotten a real/replayed order back")
                .isGreaterThanOrEqualTo(1);
    }
}
