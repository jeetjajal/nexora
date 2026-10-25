package com.nexora.phase18;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.category.entity.Category;
import com.nexora.category.repository.CategoryRepository;
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
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 18: Full Search Integration with Spring Boot & Fallback")
class SearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Store savedStore;
    private Product savedProduct;
    private Category savedCategory;

    @BeforeEach
    void setUp() {
        Role ownerRole = roleRepository.findByName(RoleName.STORE_OWNER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.STORE_OWNER).build()));

        User owner = userRepository.save(User.builder()
                .name("Search Store Owner")
                .email("searchowner_" + UUID.randomUUID() + "@nexora.test")
                .password("hash123")
                .phone("9876543210")
                .roles(Set.of(ownerRole))
                .build());

        savedCategory = categoryRepository.save(Category.builder()
                .name("SearchItalian_" + UUID.randomUUID())
                .build());

        savedStore = storeRepository.save(Store.builder()
                .name("La Piazza Gourmet")
                .description("Authentic woodfired pizza and pasta")
                .owner(owner)
                .status(StoreStatus.OPEN)
                .categories(Set.of(savedCategory))
                .build());

        savedProduct = productRepository.save(Product.builder()
                .name("Woodfired Truffle Pizza")
                .description("Black truffle paste with mozzarella")
                .price(BigDecimal.valueOf(450.0))
                .store(savedStore)
                .categories(Set.of(savedCategory))
                .available(true)
                .build());
    }

    @Test
    @DisplayName("GET /api/v1/search/products: queries products via search engine with fallback")
    void searchProducts_Integration() throws Exception {
        mockMvc.perform(get("/api/v1/search/products")
                        .param("query", "Truffle")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Woodfired Truffle Pizza"))
                .andExpect(jsonPath("$.content[0].price").value(450.0));
    }

    @Test
    @DisplayName("GET /api/v1/search/stores: queries stores via search engine with fallback")
    void searchStores_Integration() throws Exception {
        mockMvc.perform(get("/api/v1/search/stores")
                        .param("query", "Piazza")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("La Piazza Gourmet"))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"));
    }

    @Test
    @DisplayName("GET /api/v1/search/autocomplete: returns instant suggestions for prefix")
    void autocomplete_Integration() throws Exception {
        mockMvc.perform(get("/api/v1/search/autocomplete")
                        .param("prefix", "Wood")
                        .param("limit", "5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("Wood"))
                .andExpect(jsonPath("$.suggestions").isArray());
    }
}
