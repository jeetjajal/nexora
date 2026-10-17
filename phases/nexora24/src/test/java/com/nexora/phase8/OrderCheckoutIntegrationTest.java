package com.nexora.phase8;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.auth.security.JwtService;
import com.nexora.auth.security.UserPrincipal;
import com.nexora.inventory.entity.Inventory;
import com.nexora.inventory.repository.InventoryRepository;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PHASE 8: the full checkout-to-order flow through the real Spring
 * Security chain (JWT, from Phase 3), against H2 — proving multi-store
 * cart splitting, real stock consumption (reusing Phase 6's atomic
 * operation), price snapshotting (vs. Cart's live pricing from
 * Phase 7), the order state machine, cross-role visibility, and
 * customer self-cancel all work together end-to-end.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 8: Checkout and order management end-to-end tests")
class OrderCheckoutIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private StoreRepository storeRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private InventoryRepository inventoryRepository;

    private String customerToken;
    private String customerTwoToken;
    private String ownerToken;
    private String otherOwnerToken;
    private String adminToken;

    private User customer;
    private User customerTwo;
    private User owner;
    private User otherOwner;

    @BeforeEach
    void seedUsersAndTokens() {
        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.CUSTOMER).build()));
        Role storeOwnerRole = roleRepository.findByName(RoleName.STORE_OWNER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.STORE_OWNER).build()));
        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ADMIN).build()));

        customer = seedUser("customer", customerRole);
        customerTwo = seedUser("customer-two", customerRole);
        owner = seedUser("owner", storeOwnerRole);
        otherOwner = seedUser("other-owner", storeOwnerRole);
        User admin = seedUser("admin", adminRole);

        customerToken = jwtService.generateToken(new UserPrincipal(customer));
        customerTwoToken = jwtService.generateToken(new UserPrincipal(customerTwo));
        ownerToken = jwtService.generateToken(new UserPrincipal(owner));
        otherOwnerToken = jwtService.generateToken(new UserPrincipal(otherOwner));
        adminToken = jwtService.generateToken(new UserPrincipal(admin));
    }

    private User seedUser(String prefix, Role role) {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        return userRepository.save(User.builder()
                .name(prefix + " test user")
                .email(prefix + "." + unique + "@example.com")
                .password(passwordEncoder.encode("Password123"))
                .phone("9000000000")
                .status("ACTIVE")
                .roles(Set.of(role))
                .build());
    }

    private Product seedProduct(User storeOwner, BigDecimal price, int stock) {
        Store store = storeRepository.save(Store.builder()
                .owner(storeOwner)
                .name("Order Test Store " + UUID.randomUUID().toString().substring(0, 8))
                .status(StoreStatus.OPEN)
                .build());

        Product product = productRepository.save(Product.builder()
                .store(store)
                .name("Order Test Product " + UUID.randomUUID().toString().substring(0, 8))
                .price(price)
                .available(true)
                .build());

        inventoryRepository.save(Inventory.builder().product(product).quantity(stock).build());
        return product;
    }

    /** Creates a saved address for the given customer token, returns its id. */
    private Long seedAddress(String customerAuthToken) throws Exception {
        String responseJson = mockMvc.perform(post("/api/v1/addresses")
                        .header("Authorization", "Bearer " + customerAuthToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "addressLine1", "12 MG Road",
                                "city", "Rajkot",
                                "state", "Gujarat",
                                "pincode", "360001"))))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(responseJson).path("data").path("id").asLong();
    }

    private void addToCart(String customerAuthToken, Long productId, int quantity) throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customerAuthToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", productId, "quantity", quantity))))
                .andExpect(status().isCreated());
    }

    // ==============================================================
    // Address prerequisite
    // ==============================================================

    @Test
    @DisplayName("A customer can create and list their own saved addresses")
    void customerCanManageAddresses() throws Exception {
        mockMvc.perform(post("/api/v1/addresses")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "label", "Home",
                                "addressLine1", "45 Tech Park",
                                "city", "Rajkot", "state", "Gujarat", "pincode", "360005"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.city").value("Rajkot"));

        mockMvc.perform(get("/api/v1/addresses")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    // ==============================================================
    // Checkout — happy path, single store
    // ==============================================================

    @Test
    @DisplayName("Checkout creates an order, reduces stock, and clears the cart")
    void checkoutHappyPath() throws Exception {
        Product product = seedProduct(owner, new BigDecimal("100.00"), 10);
        Long addressId = seedAddress(customerToken);
        addToCart(customerToken, product.getId(), 2);

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("deliveryAddressId", addressId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("CREATED"))
                .andExpect(jsonPath("$.data[0].subtotal").value(200.00))     // 100 * 2
                .andExpect(jsonPath("$.data[0].deliveryFee").value(40.00))
                .andExpect(jsonPath("$.data[0].totalAmount").value(240.00)); // 200 + 40

        // Stock was really reduced (Phase 6's atomic operation, reused)
        Inventory inventory = inventoryRepository.findByProductId(product.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(inventory.getQuantity()).isEqualTo(8);

        // Cart was cleared
        mockMvc.perform(get("/api/v1/cart")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    @DisplayName("Checkout with an empty cart returns 400")
    void checkoutWithEmptyCartReturns400() throws Exception {
        Long addressId = seedAddress(customerToken);

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("deliveryAddressId", addressId))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Checkout with someone else's address id is rejected with 403")
    void checkoutWithAnotherUsersAddressRejected() throws Exception {
        Product product = seedProduct(owner, new BigDecimal("50.00"), 5);
        Long customerTwosAddressId = seedAddress(customerTwoToken);
        addToCart(customerToken, product.getId(), 1);

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken) // customer ONE
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("deliveryAddressId", customerTwosAddressId))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Checkout requesting more than available stock rolls back entirely (409, no order created)")
    void checkoutExceedingStockRollsBack() throws Exception {
        Product product = seedProduct(owner, new BigDecimal("75.00"), 1); // only 1 in stock
        Long addressId = seedAddress(customerToken);
        addToCart(customerToken, product.getId(), 1); // cart validated against stock at add-time, so add exactly 1

        // Simulate someone else buying it first, dropping stock to 0,
        // AFTER it was already added to this customer's cart.
        inventoryRepository.findByProductId(product.getId()).ifPresent(inv -> {
            inv.setQuantity(0);
            inventoryRepository.save(inv);
        });

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("deliveryAddressId", addressId))))
                .andExpect(status().isConflict());

        // Nothing should have been ordered
        mockMvc.perform(get("/api/v1/orders")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }

    // ==============================================================
    // Multi-store cart splitting
    // ==============================================================

    @Test
    @DisplayName("Checking out a cart with products from two different stores creates two separate orders")
    void checkoutSplitsIntoOnePerStore() throws Exception {
        Product productFromOwner = seedProduct(owner, new BigDecimal("100.00"), 10);
        Product productFromOtherOwner = seedProduct(otherOwner, new BigDecimal("60.00"), 10);
        Long addressId = seedAddress(customerToken);

        addToCart(customerToken, productFromOwner.getId(), 1);
        addToCart(customerToken, productFromOtherOwner.getId(), 1);

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("deliveryAddressId", addressId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.length()").value(2)); // TWO orders, one per store
    }

    // ==============================================================
    // Price snapshotting — the key difference from Cart
    // ==============================================================

    @Test
    @DisplayName("An order's price is snapshotted at checkout and does NOT change if the product's price changes later")
    void orderPriceIsSnapshotted() throws Exception {
        Product product = seedProduct(owner, new BigDecimal("100.00"), 10);
        Long addressId = seedAddress(customerToken);
        addToCart(customerToken, product.getId(), 1);

        String checkoutResponseJson = mockMvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("deliveryAddressId", addressId))))
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(checkoutResponseJson).path("data").get(0).path("id").asLong();

        // Store owner changes the price AFTER the order was placed
        product.setPrice(new BigDecimal("500.00"));
        productRepository.save(product);

        // The already-placed order must still show the ORIGINAL price
        mockMvc.perform(get("/api/v1/orders/" + orderId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(100.00))
                .andExpect(jsonPath("$.data.subtotal").value(100.00));
    }

    // ==============================================================
    // Visibility / authorization
    // ==============================================================

    @Test
    @DisplayName("A customer can view their own order; a different customer cannot")
    void orderVisibilityForCustomers() throws Exception {
        Product product = seedProduct(owner, new BigDecimal("40.00"), 10);
        Long addressId = seedAddress(customerToken);
        addToCart(customerToken, product.getId(), 1);

        String checkoutJson = mockMvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("deliveryAddressId", addressId))))
                .andReturn().getResponse().getContentAsString();
        Long orderId = objectMapper.readTree(checkoutJson).path("data").get(0).path("id").asLong();

        mockMvc.perform(get("/api/v1/orders/" + orderId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/orders/" + orderId)
                        .header("Authorization", "Bearer " + customerTwoToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("The owning store's owner can view the order; a different store owner cannot")
    void orderVisibilityForStoreOwners() throws Exception {
        Product product = seedProduct(owner, new BigDecimal("40.00"), 10);
        Long addressId = seedAddress(customerToken);
        addToCart(customerToken, product.getId(), 1);

        String checkoutJson = mockMvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("deliveryAddressId", addressId))))
                .andReturn().getResponse().getContentAsString();
        Long orderId = objectMapper.readTree(checkoutJson).path("data").get(0).path("id").asLong();

        mockMvc.perform(get("/api/v1/orders/" + orderId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/orders/" + orderId)
                        .header("Authorization", "Bearer " + otherOwnerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN can view any order")
    void adminCanViewAnyOrder() throws Exception {
        Product product = seedProduct(owner, new BigDecimal("40.00"), 10);
        Long addressId = seedAddress(customerToken);
        addToCart(customerToken, product.getId(), 1);

        String checkoutJson = mockMvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("deliveryAddressId", addressId))))
                .andReturn().getResponse().getContentAsString();
        Long orderId = objectMapper.readTree(checkoutJson).path("data").get(0).path("id").asLong();

        mockMvc.perform(get("/api/v1/orders/" + orderId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    // ==============================================================
    // Status transitions via the API
    // ==============================================================

    @Test
    @DisplayName("Store owner can move a fresh order through valid statuses")
    void storeOwnerCanAdvanceStatus() throws Exception {
        Product product = seedProduct(owner, new BigDecimal("40.00"), 10);
        Long addressId = seedAddress(customerToken);
        addToCart(customerToken, product.getId(), 1);

        String checkoutJson = mockMvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("deliveryAddressId", addressId))))
                .andReturn().getResponse().getContentAsString();
        Long orderId = objectMapper.readTree(checkoutJson).path("data").get(0).path("id").asLong();

        mockMvc.perform(patch("/api/v1/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "PAYMENT_PENDING"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAYMENT_PENDING"));
    }

    @Test
    @DisplayName("Invalid status transition (DELIVERED -> PREPARING) returns 409 via the API")
    void invalidTransitionViaApiReturns409() throws Exception {
        Product product = seedProduct(owner, new BigDecimal("40.00"), 10);
        Long addressId = seedAddress(customerToken);
        addToCart(customerToken, product.getId(), 1);

        String checkoutJson = mockMvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("deliveryAddressId", addressId))))
                .andReturn().getResponse().getContentAsString();
        Long orderId = objectMapper.readTree(checkoutJson).path("data").get(0).path("id").asLong();

        mockMvc.perform(patch("/api/v1/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "DELIVERED"))))
                .andExpect(status().isConflict()); // can't skip straight from CREATED to DELIVERED
    }

    @Test
    @DisplayName("A CUSTOMER cannot call the store-side status endpoint (403)")
    void customerCannotUseStatusEndpoint() throws Exception {
        Product product = seedProduct(owner, new BigDecimal("40.00"), 10);
        Long addressId = seedAddress(customerToken);
        addToCart(customerToken, product.getId(), 1);

        String checkoutJson = mockMvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("deliveryAddressId", addressId))))
                .andReturn().getResponse().getContentAsString();
        Long orderId = objectMapper.readTree(checkoutJson).path("data").get(0).path("id").asLong();

        mockMvc.perform(patch("/api/v1/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "PAYMENT_PENDING"))))
                .andExpect(status().isForbidden());
    }

    // ==============================================================
    // Customer self-cancel + stock restoration
    // ==============================================================

    @Test
    @DisplayName("Customer can cancel their own fresh order, and stock is restored")
    void customerCanCancelAndStockIsRestored() throws Exception {
        Product product = seedProduct(owner, new BigDecimal("40.00"), 5);
        Long addressId = seedAddress(customerToken);
        addToCart(customerToken, product.getId(), 2);

        String checkoutJson = mockMvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("deliveryAddressId", addressId))))
                .andReturn().getResponse().getContentAsString();
        Long orderId = objectMapper.readTree(checkoutJson).path("data").get(0).path("id").asLong();

        // Stock dropped from 5 to 3 after checkout
        org.assertj.core.api.Assertions.assertThat(
                inventoryRepository.findByProductId(product.getId()).orElseThrow().getQuantity()).isEqualTo(3);

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        // Stock restored back to 5
        org.assertj.core.api.Assertions.assertThat(
                inventoryRepository.findByProductId(product.getId()).orElseThrow().getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("Customer cannot cancel once the store has moved the order to PREPARING")
    void customerCannotCancelAfterPreparing() throws Exception {
        Product product = seedProduct(owner, new BigDecimal("40.00"), 10);
        Long addressId = seedAddress(customerToken);
        addToCart(customerToken, product.getId(), 1);

        String checkoutJson = mockMvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("deliveryAddressId", addressId))))
                .andReturn().getResponse().getContentAsString();
        Long orderId = objectMapper.readTree(checkoutJson).path("data").get(0).path("id").asLong();

        // Store advances it: CREATED -> PAYMENT_PENDING -> PAID -> CONFIRMED -> PREPARING
        for (String status : new String[]{"PAYMENT_PENDING", "PAID", "CONFIRMED", "PREPARING"}) {
            mockMvc.perform(patch("/api/v1/orders/" + orderId + "/status")
                            .header("Authorization", "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("status", status))))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isConflict());
    }

    // ==============================================================
    // Product deletion guard (Phase 4 + Phase 8 interaction)
    // ==============================================================

    @Test
    @DisplayName("A product referenced by an order cannot be hard-deleted")
    void productReferencedByOrderCannotBeDeleted() throws Exception {
        Product product = seedProduct(owner, new BigDecimal("40.00"), 10);
        Long addressId = seedAddress(customerToken);
        addToCart(customerToken, product.getId(), 1);

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("deliveryAddressId", addressId))))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/products/" + product.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isConflict());
    }
}
