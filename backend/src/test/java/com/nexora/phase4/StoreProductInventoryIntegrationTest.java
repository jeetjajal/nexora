package com.nexora.phase4;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.auth.security.JwtService;
import com.nexora.auth.security.UserPrincipal;
import com.nexora.category.entity.Category;
import com.nexora.category.repository.CategoryRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * WHY SEED USERS DIRECTLY VIA REPOSITORIES INSTEAD OF THE REGISTER API?
 * POST /api/v1/auth/register (Phase 1/2) always assigns the CUSTOMER
 * role — Nexora has no "create a STORE_OWNER/ADMIN account" endpoint
 * yet (that's a later-phase concern: an admin management surface).
 * For Phase 4 tests, we seed users with whatever role we need directly
 * through UserRepository/RoleRepository, then mint a real JWT for them
 * via JwtService — exactly the token shape AuthService would have
 * produced from a real login. This exercises the REAL
 * JwtAuthenticationFilter/SecurityConfig/@PreAuthorize chain on every
 * request; only the "how did this user get their token" step is
 * short-circuited for test setup convenience.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 4: Store/Category/Product/Inventory management tests")
class StoreProductInventoryIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private StoreRepository storeRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private InventoryRepository inventoryRepository;

    private String adminToken;
    private String ownerOneToken;
    private String ownerTwoToken;
    private String customerToken;

    private User ownerOne;
    private User ownerTwo;

    @BeforeEach
    void seedUsersAndTokens() {
        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ADMIN).build()));
        Role storeOwnerRole = roleRepository.findByName(RoleName.STORE_OWNER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.STORE_OWNER).build()));
        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.CUSTOMER).build()));

        User admin = seedUser("admin", adminRole);
        ownerOne = seedUser("owner-one", storeOwnerRole);
        ownerTwo = seedUser("owner-two", storeOwnerRole);
        User customer = seedUser("customer", customerRole);

        adminToken = jwtService.generateToken(new UserPrincipal(admin));
        ownerOneToken = jwtService.generateToken(new UserPrincipal(ownerOne));
        ownerTwoToken = jwtService.generateToken(new UserPrincipal(ownerTwo));
        customerToken = jwtService.generateToken(new UserPrincipal(customer));
    }

    private User seedUser(String prefix, Role role) {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        User user = User.builder()
                .name(prefix + " test user")
                .email(prefix + "." + unique + "@example.com")
                .password(passwordEncoder.encode("Password123"))
                .phone("9000000000")
                .status("ACTIVE")
                .roles(Set.of(role))
                .build();
        return userRepository.save(user);
    }

    private String uniqueName(String base) {
        return base + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    // ==============================================================
    // CATEGORY — ADMIN authorization
    // ==============================================================

    @Test
    @DisplayName("ADMIN can create a category")
    void adminCanCreateCategory() throws Exception {
        String name = uniqueName("Pizza");

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value(name));

        assertThat(categoryRepository.findByName(name)).isPresent();
    }

    @Test
    @DisplayName("STORE_OWNER cannot create a category (403)")
    void storeOwnerCannotCreateCategory() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + ownerOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", uniqueName("Desserts")))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CUSTOMER cannot create a category (403)")
    void customerCannotCreateCategory() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", uniqueName("Drinks")))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Creating a category WITHOUT a token returns 401")
    void createCategoryWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", uniqueName("NoAuth")))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Creating a category WITH an invalid/garbage token returns 401")
    void createCategoryWithInvalidTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer not.a.valid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", uniqueName("BadToken")))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Any authenticated role (e.g. CUSTOMER) can list categories")
    void customerCanListCategories() throws Exception {
        mockMvc.perform(get("/api/v1/categories")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Creating a duplicate category name returns 409")
    void duplicateCategoryNameReturns409() throws Exception {
        String name = uniqueName("Bakery");

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("ADMIN cannot delete a category that's still attached to a store (409)")
    void deletingCategoryInUseReturns409() throws Exception {
        Category category = categoryRepository.save(Category.builder().name(uniqueName("InUse")).build());

        storeRepository.save(Store.builder()
                .owner(ownerOne)
                .name(uniqueName("Store Using Category"))
                .status(StoreStatus.OPEN)
                .categories(Set.of(category))
                .build());

        mockMvc.perform(delete("/api/v1/categories/" + category.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());

        assertThat(categoryRepository.findById(category.getId())).isPresent();
    }

    // ==============================================================
    // STORE — STORE_OWNER / ADMIN authorization + ownership
    // ==============================================================

    @Test
    @DisplayName("STORE_OWNER can create a store, and it's persisted with the correct owner")
    void storeOwnerCanCreateStore() throws Exception {
        String storeName = uniqueName("Rajkot Pizza Hub");

        String responseJson = mockMvc.perform(post("/api/v1/stores")
                        .header("Authorization", "Bearer " + ownerOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", storeName))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value(storeName))
                .andReturn().getResponse().getContentAsString();

        Long storeId = objectMapper.readTree(responseJson).path("data").path("id").asLong();

        Store persisted = storeRepository.findById(storeId).orElseThrow();
        assertThat(persisted.getOwner().getId()).isEqualTo(ownerOne.getId());
    }

    @Test
    @DisplayName("CUSTOMER cannot create a store (403)")
    void customerCannotCreateStore() throws Exception {
        mockMvc.perform(post("/api/v1/stores")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", uniqueName("Customer Store")))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Creating a store WITHOUT a token returns 401")
    void createStoreWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", uniqueName("No Token Store")))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A STORE_OWNER can update their OWN store")
    void ownerCanUpdateOwnStore() throws Exception {
        Store store = storeRepository.save(Store.builder()
                .owner(ownerOne).name(uniqueName("Original Name")).status(StoreStatus.OPEN).build());

        String updatedName = uniqueName("Updated Name");

        mockMvc.perform(put("/api/v1/stores/" + store.getId())
                        .header("Authorization", "Bearer " + ownerOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", updatedName))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value(updatedName));

        assertThat(storeRepository.findById(store.getId()).orElseThrow().getName()).isEqualTo(updatedName);
    }

    @Test
    @DisplayName("A STORE_OWNER CANNOT update another owner's store (403)")
    void ownerCannotUpdateAnotherOwnersStore() throws Exception {
        Store store = storeRepository.save(Store.builder()
                .owner(ownerOne).name(uniqueName("Owner One Store")).status(StoreStatus.OPEN).build());

        mockMvc.perform(put("/api/v1/stores/" + store.getId())
                        .header("Authorization", "Bearer " + ownerTwoToken) // different owner!
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Hijacked Name"))))
                .andExpect(status().isForbidden());

        // Confirm nothing actually changed in the database
        assertThat(storeRepository.findById(store.getId()).orElseThrow().getName())
                .isNotEqualTo("Hijacked Name");
    }

    @Test
    @DisplayName("ADMIN can update ANY store, regardless of ownership")
    void adminCanUpdateAnyStore() throws Exception {
        Store store = storeRepository.save(Store.builder()
                .owner(ownerOne).name(uniqueName("Some Owner Store")).status(StoreStatus.OPEN).build());

        String updatedName = uniqueName("Admin Updated Name");

        mockMvc.perform(put("/api/v1/stores/" + store.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", updatedName))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value(updatedName));
    }

    @Test
    @DisplayName("A STORE_OWNER can change their own store's status")
    void ownerCanUpdateOwnStoreStatus() throws Exception {
        Store store = storeRepository.save(Store.builder()
                .owner(ownerOne).name(uniqueName("Status Store")).status(StoreStatus.OPEN).build());

        mockMvc.perform(patch("/api/v1/stores/" + store.getId() + "/status")
                        .header("Authorization", "Bearer " + ownerOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "CLOSED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        assertThat(storeRepository.findById(store.getId()).orElseThrow().getStatus())
                .isEqualTo(StoreStatus.CLOSED);
    }

    @Test
    @DisplayName("Any authenticated role can browse/view stores (read access)")
    void customerCanBrowseStores() throws Exception {
        storeRepository.save(Store.builder()
                .owner(ownerOne).name(uniqueName("Browsable Store")).status(StoreStatus.OPEN).build());

        mockMvc.perform(get("/api/v1/stores")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Fetching a non-existent store returns 404")
    void fetchingUnknownStoreReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/stores/999999")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isNotFound());
    }

    // ==============================================================
    // PRODUCT — nested under store, ownership-checked
    // ==============================================================

    @Test
    @DisplayName("A STORE_OWNER can create a product under their OWN store, with inventory seeded")
    void ownerCanCreateProductUnderOwnStore() throws Exception {
        Store store = storeRepository.save(Store.builder()
                .owner(ownerOne).name(uniqueName("Product Test Store")).status(StoreStatus.OPEN).build());

        Map<String, Object> request = Map.of(
                "name", "Margherita Pizza",
                "price", new BigDecimal("249.00"),
                "initialStock", 15
        );

        String responseJson = mockMvc.perform(post("/api/v1/stores/" + store.getId() + "/products")
                        .header("Authorization", "Bearer " + ownerOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Margherita Pizza"))
                .andExpect(jsonPath("$.data.available").value(true))
                .andReturn().getResponse().getContentAsString();

        Long productId = objectMapper.readTree(responseJson).path("data").path("id").asLong();

        Product persisted = productRepository.findById(productId).orElseThrow();
        assertThat(persisted.getStore().getId()).isEqualTo(store.getId());

        // Verify the paired Inventory row was created with the requested starting stock
        var inventory = inventoryRepository.findByProductId(productId).orElseThrow();
        assertThat(inventory.getQuantity()).isEqualTo(15);
    }

    @Test
    @DisplayName("A STORE_OWNER CANNOT create a product under another owner's store (403)")
    void ownerCannotCreateProductUnderAnotherOwnersStore() throws Exception {
        Store store = storeRepository.save(Store.builder()
                .owner(ownerOne).name(uniqueName("Owner One's Store")).status(StoreStatus.OPEN).build());

        Map<String, Object> request = Map.of("name", "Sneaky Product", "price", new BigDecimal("99.00"));

        mockMvc.perform(post("/api/v1/stores/" + store.getId() + "/products")
                        .header("Authorization", "Bearer " + ownerTwoToken) // not the owner!
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertThat(productRepository.findByStoreId(store.getId())).isEmpty();
    }

    @Test
    @DisplayName("CUSTOMER cannot create a product (403)")
    void customerCannotCreateProduct() throws Exception {
        Store store = storeRepository.save(Store.builder()
                .owner(ownerOne).name(uniqueName("Store For Customer Test")).status(StoreStatus.OPEN).build());

        Map<String, Object> request = Map.of("name", "Customer Product", "price", new BigDecimal("50.00"));

        mockMvc.perform(post("/api/v1/stores/" + store.getId() + "/products")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Any authenticated role can browse products for a store")
    void customerCanBrowseStoreProducts() throws Exception {
        Store store = storeRepository.save(Store.builder()
                .owner(ownerOne).name(uniqueName("Browse Products Store")).status(StoreStatus.OPEN).build());

        Product product = productRepository.save(Product.builder()
                .store(store).name("Visible Product").price(new BigDecimal("100.00")).build());
        inventoryRepository.save(com.nexora.inventory.entity.Inventory.builder()
                .product(product).quantity(5).build());

        mockMvc.perform(get("/api/v1/stores/" + store.getId() + "/products")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("Visible Product"));
    }

    @Test
    @DisplayName("A STORE_OWNER can deactivate their own product (soft delete)")
    void ownerCanDeactivateOwnProduct() throws Exception {
        Store store = storeRepository.save(Store.builder()
                .owner(ownerOne).name(uniqueName("Deactivate Test Store")).status(StoreStatus.OPEN).build());

        Product product = productRepository.save(Product.builder()
                .store(store).name("Soon Deactivated").price(new BigDecimal("75.00")).available(true).build());
        inventoryRepository.save(com.nexora.inventory.entity.Inventory.builder()
                .product(product).quantity(10).build());

        mockMvc.perform(patch("/api/v1/products/" + product.getId() + "/deactivate")
                        .header("Authorization", "Bearer " + ownerOneToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(false));

        assertThat(productRepository.findById(product.getId()).orElseThrow().isAvailable()).isFalse();
    }

    // ==============================================================
    // INVENTORY — stock check and update, ownership-checked
    // ==============================================================

    @Test
    @DisplayName("A STORE_OWNER can update stock for their own product")
    void ownerCanUpdateOwnProductStock() throws Exception {
        Store store = storeRepository.save(Store.builder()
                .owner(ownerOne).name(uniqueName("Stock Test Store")).status(StoreStatus.OPEN).build());

        Product product = productRepository.save(Product.builder()
                .store(store).name("Stock Test Product").price(new BigDecimal("60.00")).build());

        var inventory = inventoryRepository.save(com.nexora.inventory.entity.Inventory.builder()
                .product(product).quantity(5).build());

        mockMvc.perform(put("/api/v1/products/" + product.getId() + "/inventory")
                        .header("Authorization", "Bearer " + ownerOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("quantity", 40))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(40));

        assertThat(inventoryRepository.findById(inventory.getId()).orElseThrow().getQuantity()).isEqualTo(40);
    }

    @Test
    @DisplayName("A STORE_OWNER CANNOT update stock for another owner's product (403)")
    void ownerCannotUpdateAnotherOwnersProductStock() throws Exception {
        Store store = storeRepository.save(Store.builder()
                .owner(ownerOne).name(uniqueName("Foreign Stock Store")).status(StoreStatus.OPEN).build());

        Product product = productRepository.save(Product.builder()
                .store(store).name("Foreign Stock Product").price(new BigDecimal("60.00")).build());

        inventoryRepository.save(com.nexora.inventory.entity.Inventory.builder()
                .product(product).quantity(5).build());

        mockMvc.perform(put("/api/v1/products/" + product.getId() + "/inventory")
                        .header("Authorization", "Bearer " + ownerTwoToken) // not the owner!
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("quantity", 999))))
                .andExpect(status().isForbidden());

        assertThat(inventoryRepository.findByProductId(product.getId()).orElseThrow().getQuantity())
                .isEqualTo(5); // unchanged
    }

    @Test
    @DisplayName("Setting inventory to a negative quantity is rejected with 400")
    void negativeStockQuantityRejected() throws Exception {
        Store store = storeRepository.save(Store.builder()
                .owner(ownerOne).name(uniqueName("Negative Stock Store")).status(StoreStatus.OPEN).build());

        Product product = productRepository.save(Product.builder()
                .store(store).name("Negative Stock Product").price(new BigDecimal("30.00")).build());

        inventoryRepository.save(com.nexora.inventory.entity.Inventory.builder()
                .product(product).quantity(3).build());

        mockMvc.perform(put("/api/v1/products/" + product.getId() + "/inventory")
                        .header("Authorization", "Bearer " + ownerOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("quantity", -5))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Any authenticated role can check product stock/availability")
    void customerCanCheckProductAvailability() throws Exception {
        Store store = storeRepository.save(Store.builder()
                .owner(ownerOne).name(uniqueName("Availability Store")).status(StoreStatus.OPEN).build());

        Product product = productRepository.save(Product.builder()
                .store(store).name("Availability Product").price(new BigDecimal("20.00")).build());

        inventoryRepository.save(com.nexora.inventory.entity.Inventory.builder()
                .product(product).quantity(8).build());

        mockMvc.perform(get("/api/v1/products/" + product.getId() + "/inventory")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(8));
    }
}
