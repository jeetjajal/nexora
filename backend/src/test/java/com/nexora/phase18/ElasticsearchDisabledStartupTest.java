package com.nexora.phase18;

import com.nexora.category.entity.Category;
import com.nexora.category.repository.CategoryRepository;
import com.nexora.product.entity.Product;
import com.nexora.product.repository.ProductRepository;
import com.nexora.role.entity.Role;
import com.nexora.role.entity.RoleName;
import com.nexora.role.repository.RoleRepository;
import com.nexora.search.dto.AutocompleteResponse;
import com.nexora.search.dto.ProductSearchResult;
import com.nexora.search.dto.SearchSyncResponse;
import com.nexora.search.dto.StoreSearchResult;
import com.nexora.search.service.ProductSearchService;
import com.nexora.search.service.SearchSyncService;
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
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "nexora.search.elasticsearch.enabled=false",
        "spring.data.elasticsearch.repositories.enabled=false"
})
@DisplayName("Phase 18/19: Elasticsearch Disabled Mode Verification")
class ElasticsearchDisabledStartupTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ProductSearchService productSearchService;

    @Autowired
    private SearchSyncService searchSyncService;

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

    @BeforeEach
    void setUp() {
        Role ownerRole = roleRepository.findByName(RoleName.STORE_OWNER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.STORE_OWNER).build()));

        User owner = userRepository.save(User.builder()
                .name("ES Disabled Store Owner")
                .email("esdisabled_" + UUID.randomUUID() + "@nexora.test")
                .password("hash123")
                .phone("9876543210")
                .roles(Set.of(ownerRole))
                .build());

        Category category = categoryRepository.save(Category.builder()
                .name("Bakery_" + UUID.randomUUID())
                .build());

        savedStore = storeRepository.save(Store.builder()
                .name("Sweet Oven Bakery")
                .description("Fresh bread and pastries")
                .owner(owner)
                .status(StoreStatus.OPEN)
                .categories(Set.of(category))
                .build());

        savedProduct = productRepository.save(Product.builder()
                .name("Sourdough Loaf")
                .description("Artisan natural yeast bread")
                .price(BigDecimal.valueOf(180.0))
                .store(savedStore)
                .categories(Set.of(category))
                .available(true)
                .build());
    }

    @Test
    @DisplayName("Mode A: Elasticsearch repository beans are absent when disabled")
    void verifyElasticsearchRepositoriesAreAbsent() {
        assertThat(applicationContext.containsBean("productSearchRepository"))
                .as("productSearchRepository bean should NOT be registered when Elasticsearch is disabled")
                .isFalse();

        assertThat(applicationContext.containsBean("storeSearchRepository"))
                .as("storeSearchRepository bean should NOT be registered when Elasticsearch is disabled")
                .isFalse();
    }

    @Test
    @DisplayName("Mode A: Product search uses MySQL database fallback and returns DATABASE_FALLBACK source")
    void productSearch_UsesDatabaseFallback() {
        Page<ProductSearchResult> result = productSearchService.searchProducts(
                "Sourdough", null, null, null, null, null, PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotEmpty();
        ProductSearchResult first = result.getContent().get(0);
        assertThat(first.getName()).isEqualTo("Sourdough Loaf");
        assertThat(first.getSource()).isEqualTo("DATABASE_FALLBACK");
    }

    @Test
    @DisplayName("Mode A: Store search uses MySQL database fallback and returns DATABASE_FALLBACK source")
    void storeSearch_UsesDatabaseFallback() {
        Page<StoreSearchResult> result = productSearchService.searchStores(
                "Sweet Oven", null, PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotEmpty();
        StoreSearchResult first = result.getContent().get(0);
        assertThat(first.getName()).isEqualTo("Sweet Oven Bakery");
        assertThat(first.getSource()).isEqualTo("DATABASE_FALLBACK");
    }

    @Test
    @DisplayName("Mode A: Autocomplete gracefully falls back to database suggestions")
    void autocomplete_UsesDatabaseFallback() {
        AutocompleteResponse response = productSearchService.autocomplete("Sour", 5);

        assertThat(response).isNotNull();
        assertThat(response.getSuggestions()).contains("Sourdough Loaf");
    }

    @Test
    @DisplayName("Mode A: Search sync gracefully skips without throwing connection errors")
    void searchSync_SkipsGracefullyWhenRepositoriesAbsent() {
        SearchSyncResponse syncResponse = searchSyncService.syncAll();

        assertThat(syncResponse).isNotNull();
        assertThat(syncResponse.getStatus()).isEqualTo("SKIPPED");
        assertThat(syncResponse.getMessage()).contains("Elasticsearch repositories are not active");
    }
}
