package com.nexora.phase5;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.auth.security.JwtService;
import com.nexora.auth.security.UserPrincipal;
import com.nexora.category.entity.Category;
import com.nexora.category.repository.CategoryRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PHASE 5: search, filter, sort, pagination — plus discount validation
 * and a handful of regression spot-checks confirming Phase 4's
 * ownership/authorization rules still hold under the new query-param
 * surface. Full Phase 1–4 regression coverage still lives in and is
 * re-run from UserServiceTest, RelationshipRepositoryTest,
 * JwtServiceTest/AuthServiceTest/AuthControllerIntegrationTest, and
 * StoreProductInventoryIntegrationTest — `mvn test` runs all of them
 * together every time, so nothing from earlier phases is skipped.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 5: Product/Store search, filter, sort, pagination")
class SearchFilterSortIntegrationTest {

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

    private String customerToken;
    private String ownerOneToken;
    private String ownerTwoToken;
    private User ownerOne;
    private User ownerTwo;

    @BeforeEach
    void seedUsersAndTokens() {
        Role storeOwnerRole = roleRepository.findByName(RoleName.STORE_OWNER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.STORE_OWNER).build()));
        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.CUSTOMER).build()));

        ownerOne = seedUser("owner-one", storeOwnerRole);
        ownerTwo = seedUser("owner-two", storeOwnerRole);
        User customer = seedUser("customer", customerRole);

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

    private Store seedStore(User owner, String name, Category... categories) {
        return storeRepository.save(Store.builder()
                .owner(owner).name(name).status(StoreStatus.OPEN)
                .categories(Set.of(categories))
                .build());
    }

    private Product seedProduct(Store store, String name, BigDecimal price, boolean available, int stock, Category... categories) {
        Product product = productRepository.save(Product.builder()
                .store(store).name(name).price(price).available(available)
                .categories(Set.of(categories))
                .build());
        inventoryRepository.save(Inventory.builder().product(product).quantity(stock).build());
        return product;
    }

    // ==============================================================
    // PRODUCT SEARCH / FILTER / SORT
    // ==============================================================

    @Test
    @DisplayName("Search products by name substring, case-insensitive")
    void searchProductsByNameSubstring() throws Exception {
        String uniqueMarker = UUID.randomUUID().toString().substring(0, 8);
        Store store = seedStore(ownerOne, uniqueName("Pizza Palace"));
        seedProduct(store, "Margherita Pizza " + uniqueMarker, new BigDecimal("249.00"), true, 10);
        seedProduct(store, "Veggie Burger " + uniqueMarker, new BigDecimal("149.00"), true, 10);

        mockMvc.perform(get("/api/v1/products")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("name", "pizza " + uniqueMarker)) // lowercase, partial
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("Margherita Pizza " + uniqueMarker));
    }

    @Test
    @DisplayName("Filter products by category name")
    void filterProductsByCategory() throws Exception {
        Category pizzaCategory = categoryRepository.save(Category.builder().name(uniqueName("PizzaCat")).build());
        Category drinksCategory = categoryRepository.save(Category.builder().name(uniqueName("DrinksCat")).build());

        Store store = seedStore(ownerOne, uniqueName("Category Filter Store"));
        String marker = UUID.randomUUID().toString().substring(0, 8);
        seedProduct(store, "Pizza Item " + marker, new BigDecimal("200.00"), true, 5, pizzaCategory);
        seedProduct(store, "Cola Item " + marker, new BigDecimal("50.00"), true, 5, drinksCategory);

        mockMvc.perform(get("/api/v1/products")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("categoryName", pizzaCategory.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.name == 'Pizza Item " + marker + "')]").exists())
                .andExpect(jsonPath("$.data.content[?(@.name == 'Cola Item " + marker + "')]").doesNotExist());
    }

    @Test
    @DisplayName("Filter products by price range (min and max)")
    void filterProductsByPriceRange() throws Exception {
        Store store = seedStore(ownerOne, uniqueName("Price Range Store"));
        String marker = UUID.randomUUID().toString().substring(0, 8);
        seedProduct(store, "Cheap Item " + marker, new BigDecimal("50.00"), true, 5);
        seedProduct(store, "Mid Item " + marker, new BigDecimal("150.00"), true, 5);
        seedProduct(store, "Expensive Item " + marker, new BigDecimal("500.00"), true, 5);

        mockMvc.perform(get("/api/v1/products")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("minPrice", "100")
                        .param("maxPrice", "200")
                        .param("name", marker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("Mid Item " + marker));
    }

    @Test
    @DisplayName("Filter products by availability")
    void filterProductsByAvailability() throws Exception {
        Store store = seedStore(ownerOne, uniqueName("Availability Filter Store"));
        String marker = UUID.randomUUID().toString().substring(0, 8);
        seedProduct(store, "Available Item " + marker, new BigDecimal("80.00"), true, 5);
        seedProduct(store, "Unavailable Item " + marker, new BigDecimal("80.00"), false, 0);

        mockMvc.perform(get("/api/v1/products")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("available", "false")
                        .param("name", marker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("Unavailable Item " + marker));
    }

    @Test
    @DisplayName("Sort products by price ascending")
    void sortProductsByPriceAscending() throws Exception {
        Store store = seedStore(ownerOne, uniqueName("Sort Store"));
        String marker = UUID.randomUUID().toString().substring(0, 8);
        seedProduct(store, "Z Item " + marker, new BigDecimal("300.00"), true, 5);
        seedProduct(store, "A Item " + marker, new BigDecimal("100.00"), true, 5);
        seedProduct(store, "M Item " + marker, new BigDecimal("200.00"), true, 5);

        mockMvc.perform(get("/api/v1/products")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("name", marker)
                        .param("sort", "price,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("A Item " + marker))
                .andExpect(jsonPath("$.data.content[1].name").value("M Item " + marker))
                .andExpect(jsonPath("$.data.content[2].name").value("Z Item " + marker));
    }

    @Test
    @DisplayName("Combining multiple filters at once (name + price range + availability)")
    void combineMultipleFilters() throws Exception {
        Store store = seedStore(ownerOne, uniqueName("Combo Filter Store"));
        String marker = UUID.randomUUID().toString().substring(0, 8);
        seedProduct(store, "Match Combo " + marker, new BigDecimal("120.00"), true, 5);
        seedProduct(store, "Wrong Price Combo " + marker, new BigDecimal("999.00"), true, 5);
        seedProduct(store, "Wrong Availability Combo " + marker, new BigDecimal("120.00"), false, 0);

        mockMvc.perform(get("/api/v1/products")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("name", "combo " + marker)
                        .param("minPrice", "100")
                        .param("maxPrice", "150")
                        .param("available", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("Match Combo " + marker));
    }

    @Test
    @DisplayName("Pagination limits results per page and reports total correctly")
    void paginationWorksCorrectly() throws Exception {
        Store store = seedStore(ownerOne, uniqueName("Pagination Store"));
        String marker = UUID.randomUUID().toString().substring(0, 8);
        for (int i = 0; i < 5; i++) {
            seedProduct(store, "Page Item " + i + " " + marker, new BigDecimal("100.00"), true, 5);
        }

        mockMvc.perform(get("/api/v1/products")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("name", marker)
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(5))
                .andExpect(jsonPath("$.data.totalPages").value(3));
    }

    // ==============================================================
    // STORE SEARCH / FILTER
    // ==============================================================

    @Test
    @DisplayName("Search stores by name substring")
    void searchStoresByName() throws Exception {
        String marker = UUID.randomUUID().toString().substring(0, 8);
        seedStore(ownerOne, "Rajkot Pizza Hub " + marker);
        seedStore(ownerOne, "Green Grocery " + marker);

        mockMvc.perform(get("/api/v1/stores")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("name", "pizza " + marker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("Rajkot Pizza Hub " + marker));
    }

    @Test
    @DisplayName("Filter stores by category")
    void filterStoresByCategory() throws Exception {
        Category fastFood = categoryRepository.save(Category.builder().name(uniqueName("FastFoodCat")).build());
        String marker = UUID.randomUUID().toString().substring(0, 8);

        seedStore(ownerOne, "Fast Food Place " + marker, fastFood);
        seedStore(ownerOne, "Unrelated Store " + marker);

        mockMvc.perform(get("/api/v1/stores")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("categoryName", fastFood.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.name == 'Fast Food Place " + marker + "')]").exists());
    }

    @Test
    @DisplayName("Filter stores by status")
    void filterStoresByStatus() throws Exception {
        String marker = UUID.randomUUID().toString().substring(0, 8);
        seedStore(ownerOne, "Open Store " + marker);
        storeRepository.save(Store.builder()
                .owner(ownerOne).name("Closed Store " + marker).status(StoreStatus.CLOSED).build());

        mockMvc.perform(get("/api/v1/stores")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("status", "CLOSED")
                        .param("name", marker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("Closed Store " + marker));
    }

    // ==============================================================
    // VALIDATION — discount cannot exceed price (Phase 5 addition)
    // ==============================================================

    @Test
    @DisplayName("Creating a product with discount greater than price is rejected with 400")
    void discountGreaterThanPriceRejected() throws Exception {
        Store store = seedStore(ownerOne, uniqueName("Discount Validation Store"));

        Map<String, Object> request = Map.of(
                "name", "Overdiscounted Item",
                "price", new BigDecimal("100.00"),
                "discount", new BigDecimal("150.00") // more than the price itself
        );

        mockMvc.perform(post("/api/v1/stores/" + store.getId() + "/products")
                        .header("Authorization", "Bearer " + ownerOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertThat(productRepository.findByStoreId(store.getId())).isEmpty();
    }

    @Test
    @DisplayName("Creating a product with a valid discount succeeds")
    void validDiscountAccepted() throws Exception {
        Store store = seedStore(ownerOne, uniqueName("Valid Discount Store"));

        Map<String, Object> request = Map.of(
                "name", "Reasonably Discounted Item",
                "price", new BigDecimal("100.00"),
                "discount", new BigDecimal("20.00")
        );

        mockMvc.perform(post("/api/v1/stores/" + store.getId() + "/products")
                        .header("Authorization", "Bearer " + ownerOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.discount").value(20.00));
    }

    // ==============================================================
    // REGRESSION SPOT-CHECKS — Phase 4 authorization still holds
    // under the Phase 5 query-param surface
    // ==============================================================

    @Test
    @DisplayName("REGRESSION: cross-store product creation is still forbidden after Phase 5 changes")
    void crossStoreProductCreationStillForbidden() throws Exception {
        Store store = seedStore(ownerOne, uniqueName("Regression Store"));

        Map<String, Object> request = Map.of("name", "Sneaky Product", "price", new BigDecimal("99.00"));

        mockMvc.perform(post("/api/v1/stores/" + store.getId() + "/products")
                        .header("Authorization", "Bearer " + ownerTwoToken) // not the owner
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("REGRESSION: unfiltered GET /api/v1/products still returns paginated results")
    void unfilteredProductListingStillWorks() throws Exception {
        Store store = seedStore(ownerOne, uniqueName("Unfiltered Store"));
        seedProduct(store, uniqueName("Plain Product"), new BigDecimal("40.00"), true, 5);

        mockMvc.perform(get("/api/v1/products")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("REGRESSION: creating a category without ADMIN role is still forbidden")
    void categoryCreationStillAdminOnly() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + ownerOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", uniqueName("StillAdminOnly")))))
                .andExpect(status().isForbidden());
    }
}
