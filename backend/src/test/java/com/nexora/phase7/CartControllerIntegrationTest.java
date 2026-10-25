package com.nexora.phase7;

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
 * PHASE 7: full end-to-end cart flow through the real Spring Security
 * filter chain (JWT auth, from Phase 3) against H2 — proving the
 * complete CRUD surface, the "never trust the frontend price" rule,
 * insufficient-stock protection, and cross-user isolation all actually
 * work together, not just in isolation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 7: Cart end-to-end integration tests")
class CartControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private StoreRepository storeRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private InventoryRepository inventoryRepository;

    private String customerOneToken;
    private String customerTwoToken;
    private User storeOwnerUser;

    @BeforeEach
    void seedUsersAndTokens() {
        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.CUSTOMER).build()));
        Role storeOwnerRole = roleRepository.findByName(RoleName.STORE_OWNER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.STORE_OWNER).build()));

        User customerOne = seedUser("customer-one", customerRole);
        User customerTwo = seedUser("customer-two", customerRole);
        storeOwnerUser = seedUser("owner", storeOwnerRole);

        customerOneToken = jwtService.generateToken(new UserPrincipal(customerOne));
        customerTwoToken = jwtService.generateToken(new UserPrincipal(customerTwo));
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

    private Product seedProduct(BigDecimal price, BigDecimal discount, boolean available, int stock) {
        Store store = storeRepository.save(Store.builder()
                .owner(storeOwnerUser)
                .name("Cart Test Store " + UUID.randomUUID().toString().substring(0, 8))
                .status(StoreStatus.OPEN)
                .build());

        Product product = productRepository.save(Product.builder()
                .store(store)
                .name("Cart Test Product " + UUID.randomUUID().toString().substring(0, 8))
                .price(price)
                .discount(discount)
                .available(available)
                .build());

        inventoryRepository.save(Inventory.builder().product(product).quantity(stock).build());

        return product;
    }

    @Test
    @DisplayName("GET /api/v1/cart without a token returns 401")
    void getCartWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/cart for a brand-new user returns an empty cart, created lazily")
    void newUserGetsEmptyCart() throws Exception {
        mockMvc.perform(get("/api/v1/cart")
                        .header("Authorization", "Bearer " + customerOneToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(0))
                .andExpect(jsonPath("$.data.subtotal").value(0));
    }

    @Test
    @DisplayName("Adding an item to the cart persists it and computes price from the backend")
    void addingItemPersistsAndComputesPrice() throws Exception {
        Product product = seedProduct(new BigDecimal("200.00"), new BigDecimal("20.00"), true, 10);

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customerOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", product.getId(), "quantity", 2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(180.00)) // 200 - 20
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.subtotal").value(360.00));
    }

    @Test
    @DisplayName("Backend ignores any price sent in the request body — price always comes from the Product")
    void backendIgnoresClientSuppliedPrice() throws Exception {
        Product product = seedProduct(new BigDecimal("100.00"), BigDecimal.ZERO, true, 10);

        // Try to sneak a "price" field into the request — AddCartItemRequest
        // has no such field at all, so Jackson simply ignores it.
        String maliciousPayload = """
                { "productId": %d, "quantity": 1, "price": 1.00, "unitPrice": 1.00 }
                """.formatted(product.getId());

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customerOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(maliciousPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(100.00)); // real price, not 1.00
    }

    @Test
    @DisplayName("Adding the same product twice increases quantity instead of creating a duplicate row")
    void addingSameProductTwiceIncreasesQuantity() throws Exception {
        Product product = seedProduct(new BigDecimal("50.00"), BigDecimal.ZERO, true, 10);

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customerOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", product.getId(), "quantity", 1))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customerOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", product.getId(), "quantity", 2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].quantity").value(3));
    }

    @Test
    @DisplayName("Adding more than available stock is rejected with 409")
    void addingMoreThanStockReturns409() throws Exception {
        Product product = seedProduct(new BigDecimal("30.00"), BigDecimal.ZERO, true, 2);

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customerOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", product.getId(), "quantity", 5))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Adding an unavailable product is rejected with 409")
    void addingUnavailableProductReturns409() throws Exception {
        Product product = seedProduct(new BigDecimal("30.00"), BigDecimal.ZERO, false, 10);

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customerOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", product.getId(), "quantity", 1))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Updating a cart item's quantity works")
    void updatingCartItemQuantityWorks() throws Exception {
        Product product = seedProduct(new BigDecimal("40.00"), BigDecimal.ZERO, true, 10);

        String addResponseJson = mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customerOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", product.getId(), "quantity", 1))))
                .andReturn().getResponse().getContentAsString();

        Long itemId = objectMapper.readTree(addResponseJson).path("data").path("items").get(0).path("id").asLong();

        mockMvc.perform(put("/api/v1/cart/items/" + itemId)
                        .header("Authorization", "Bearer " + customerOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("quantity", 6))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].quantity").value(6))
                .andExpect(jsonPath("$.data.subtotal").value(240.00)); // 40 * 6
    }

    @Test
    @DisplayName("Removing a cart item deletes it from the cart")
    void removingCartItemWorks() throws Exception {
        Product product = seedProduct(new BigDecimal("25.00"), BigDecimal.ZERO, true, 10);

        String addResponseJson = mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customerOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", product.getId(), "quantity", 1))))
                .andReturn().getResponse().getContentAsString();

        Long itemId = objectMapper.readTree(addResponseJson).path("data").path("items").get(0).path("id").asLong();

        mockMvc.perform(delete("/api/v1/cart/items/" + itemId)
                        .header("Authorization", "Bearer " + customerOneToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    @DisplayName("Clearing the cart removes every item")
    void clearingCartRemovesEverything() throws Exception {
        Product productA = seedProduct(new BigDecimal("10.00"), BigDecimal.ZERO, true, 10);
        Product productB = seedProduct(new BigDecimal("20.00"), BigDecimal.ZERO, true, 10);

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customerOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", productA.getId(), "quantity", 1))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customerOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", productB.getId(), "quantity", 1))))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/cart")
                        .header("Authorization", "Bearer " + customerOneToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0))
                .andExpect(jsonPath("$.data.subtotal").value(0));
    }

    @Test
    @DisplayName("A user cannot update another user's cart item (cross-user isolation)")
    void cannotUpdateAnotherUsersCartItem() throws Exception {
        Product product = seedProduct(new BigDecimal("60.00"), BigDecimal.ZERO, true, 10);

        String addResponseJson = mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customerOneToken) // customer ONE adds it
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", product.getId(), "quantity", 1))))
                .andReturn().getResponse().getContentAsString();

        Long itemId = objectMapper.readTree(addResponseJson).path("data").path("items").get(0).path("id").asLong();

        // customer TWO tries to modify customer ONE's cart item
        mockMvc.perform(put("/api/v1/cart/items/" + itemId)
                        .header("Authorization", "Bearer " + customerTwoToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("quantity", 99))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A user cannot remove another user's cart item (cross-user isolation)")
    void cannotRemoveAnotherUsersCartItem() throws Exception {
        Product product = seedProduct(new BigDecimal("15.00"), BigDecimal.ZERO, true, 10);

        String addResponseJson = mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customerOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", product.getId(), "quantity", 1))))
                .andReturn().getResponse().getContentAsString();

        Long itemId = objectMapper.readTree(addResponseJson).path("data").path("items").get(0).path("id").asLong();

        mockMvc.perform(delete("/api/v1/cart/items/" + itemId)
                        .header("Authorization", "Bearer " + customerTwoToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Two different users each have their own independent cart")
    void usersHaveIndependentCarts() throws Exception {
        Product product = seedProduct(new BigDecimal("70.00"), BigDecimal.ZERO, true, 10);

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customerOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", product.getId(), "quantity", 1))))
                .andExpect(status().isCreated());

        // customer TWO's cart should still be empty
        mockMvc.perform(get("/api/v1/cart")
                        .header("Authorization", "Bearer " + customerTwoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    @DisplayName("Setting quantity to 0 via update is rejected by validation")
    void updatingQuantityToZeroRejected() throws Exception {
        Product product = seedProduct(new BigDecimal("10.00"), BigDecimal.ZERO, true, 10);

        String addResponseJson = mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customerOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", product.getId(), "quantity", 1))))
                .andReturn().getResponse().getContentAsString();

        Long itemId = objectMapper.readTree(addResponseJson).path("data").path("items").get(0).path("id").asLong();

        mockMvc.perform(put("/api/v1/cart/items/" + itemId)
                        .header("Authorization", "Bearer " + customerOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("quantity", 0))))
                .andExpect(status().isBadRequest());
    }
}
