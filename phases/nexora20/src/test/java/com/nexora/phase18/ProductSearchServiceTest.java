package com.nexora.phase18;

import com.nexora.category.entity.Category;
import com.nexora.product.entity.Product;
import com.nexora.product.repository.ProductRepository;
import com.nexora.search.document.ProductDocument;
import com.nexora.search.document.StoreDocument;
import com.nexora.search.dto.AutocompleteResponse;
import com.nexora.search.dto.ProductSearchResult;
import com.nexora.search.dto.StoreSearchResult;
import com.nexora.search.repository.ProductSearchRepository;
import com.nexora.search.repository.StoreSearchRepository;
import com.nexora.search.service.ProductSearchServiceImpl;
import com.nexora.store.entity.Store;
import com.nexora.store.entity.StoreStatus;
import com.nexora.store.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductSearchServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private ProductSearchRepository productSearchRepository;

    @Mock
    private StoreSearchRepository storeSearchRepository;

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @InjectMocks
    private ProductSearchServiceImpl searchService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(searchService, "productSearchRepository", productSearchRepository);
        ReflectionTestUtils.setField(searchService, "storeSearchRepository", storeSearchRepository);
        ReflectionTestUtils.setField(searchService, "elasticsearchOperations", elasticsearchOperations);
        ReflectionTestUtils.setField(searchService, "searchEnabled", true);
    }

    @Test
    @DisplayName("Elasticsearch: searches products using criteria and returns ELASTICSEARCH source")
    void searchProducts_ElasticsearchSuccess() {
        ProductDocument doc = ProductDocument.builder()
                .id("prod-1")
                .productId(1L)
                .name("Margherita Pizza")
                .description("Cheesy delicious pizza")
                .price(299.0)
                .discount(10.0)
                .available(true)
                .storeId(10L)
                .storeName("Pizza Palace")
                .categories(List.of("Italian", "Pizza"))
                .build();

        SearchHit<ProductDocument> hit = new SearchHit<>(null, null, null, 1.0f, null, null, null, null, null, null, doc);
        SearchHits<ProductDocument> searchHits = mock(SearchHits.class);
        when(searchHits.getSearchHits()).thenReturn(List.of(hit));
        when(searchHits.getTotalHits()).thenReturn(1L);
        when(elasticsearchOperations.search(any(CriteriaQuery.class), eq(ProductDocument.class))).thenReturn(searchHits);

        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductSearchResult> results = searchService.searchProducts(
                "pizza", "Pizza", BigDecimal.valueOf(100), BigDecimal.valueOf(500), true, 10L, pageable);

        assertThat(results).isNotNull();
        assertThat(results.getTotalElements()).isEqualTo(1L);
        ProductSearchResult item = results.getContent().get(0);
        assertThat(item.getId()).isEqualTo(1L);
        assertThat(item.getName()).isEqualTo("Margherita Pizza");
        assertThat(item.getSource()).isEqualTo("ELASTICSEARCH");
    }

    @Test
    @DisplayName("Fallback: when Elasticsearch fails, falls back seamlessly to MySQL database search")
    void searchProducts_DatabaseFallbackOnException() {
        when(elasticsearchOperations.search(any(CriteriaQuery.class), eq(ProductDocument.class)))
                .thenThrow(new RuntimeException("Elasticsearch cluster connection timeout"));

        Store store = Store.builder().id(10L).name("Pizza Palace").build();
        Product product = Product.builder()
                .id(1L)
                .name("Margherita Pizza")
                .description("Cheesy delicious pizza")
                .price(BigDecimal.valueOf(299.0))
                .discount(BigDecimal.valueOf(10.0))
                .available(true)
                .store(store)
                .categories(Set.of(Category.builder().id(1L).name("Italian").build()))
                .build();

        Page<Product> dbPage = new PageImpl<>(List.of(product), PageRequest.of(0, 10), 1);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(dbPage);

        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductSearchResult> results = searchService.searchProducts(
                "pizza", null, null, null, null, null, pageable);

        assertThat(results).isNotNull();
        assertThat(results.getTotalElements()).isEqualTo(1L);
        ProductSearchResult item = results.getContent().get(0);
        assertThat(item.getName()).isEqualTo("Margherita Pizza");
        assertThat(item.getSource()).isEqualTo("DATABASE_FALLBACK");
        verify(productRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("Elasticsearch: searches stores and returns results with ELASTICSEARCH source")
    void searchStores_ElasticsearchSuccess() {
        StoreDocument doc = StoreDocument.builder()
                .id("store-10")
                .storeId(10L)
                .name("Pizza Palace")
                .description("Best pizza in town")
                .status("OPEN")
                .categories(List.of("Italian", "Fast Food"))
                .build();

        SearchHit<StoreDocument> hit = new SearchHit<>(null, null, null, 1.0f, null, null, null, null, null, null, doc);
        SearchHits<StoreDocument> searchHits = mock(SearchHits.class);
        when(searchHits.getSearchHits()).thenReturn(List.of(hit));
        when(searchHits.getTotalHits()).thenReturn(1L);
        when(elasticsearchOperations.search(any(CriteriaQuery.class), eq(StoreDocument.class))).thenReturn(searchHits);

        Pageable pageable = PageRequest.of(0, 10);
        Page<StoreSearchResult> results = searchService.searchStores("pizza", "Italian", pageable);

        assertThat(results).isNotNull();
        assertThat(results.getTotalElements()).isEqualTo(1L);
        StoreSearchResult item = results.getContent().get(0);
        assertThat(item.getId()).isEqualTo(10L);
        assertThat(item.getName()).isEqualTo("Pizza Palace");
        assertThat(item.getSource()).isEqualTo("ELASTICSEARCH");
    }

    @Test
    @DisplayName("Fallback: when Elasticsearch fails, falls back to Store database search")
    void searchStores_DatabaseFallbackOnException() {
        when(elasticsearchOperations.search(any(CriteriaQuery.class), eq(StoreDocument.class)))
                .thenThrow(new RuntimeException("OpenSearch host unreachable"));

        Store store = Store.builder()
                .id(10L)
                .name("Pizza Palace")
                .description("Best pizza in town")
                .status(StoreStatus.OPEN)
                .categories(Set.of(Category.builder().id(1L).name("Italian").build()))
                .build();

        Page<Store> dbPage = new PageImpl<>(List.of(store), PageRequest.of(0, 10), 1);
        when(storeRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(dbPage);

        Pageable pageable = PageRequest.of(0, 10);
        Page<StoreSearchResult> results = searchService.searchStores("pizza", null, pageable);

        assertThat(results).isNotNull();
        assertThat(results.getTotalElements()).isEqualTo(1L);
        StoreSearchResult item = results.getContent().get(0);
        assertThat(item.getName()).isEqualTo("Pizza Palace");
        assertThat(item.getSource()).isEqualTo("DATABASE_FALLBACK");
        verify(storeRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("Autocomplete: returns suggestions and top matched products and stores")
    void autocomplete_ElasticsearchSuccess() {
        ProductDocument pDoc = ProductDocument.builder()
                .id("prod-1")
                .productId(1L)
                .name("Burger King Whopper")
                .price(199.0)
                .build();

        StoreDocument sDoc = StoreDocument.builder()
                .id("store-5")
                .storeId(5L)
                .name("Burger Joint")
                .status("OPEN")
                .build();

        when(productSearchRepository.findByNameStartingWithIgnoreCase("bur")).thenReturn(List.of(pDoc));
        when(storeSearchRepository.findByNameStartingWithIgnoreCase("bur")).thenReturn(List.of(sDoc));

        AutocompleteResponse response = searchService.autocomplete("bur", 10);

        assertThat(response).isNotNull();
        assertThat(response.getQuery()).isEqualTo("bur");
        assertThat(response.getSuggestions()).containsExactlyInAnyOrder("Burger King Whopper", "Burger Joint");
        assertThat(response.getProducts()).hasSize(1);
        assertThat(response.getStores()).hasSize(1);
    }

    @Test
    @DisplayName("Autocomplete: empty prefix returns empty response")
    void autocomplete_EmptyPrefix() {
        AutocompleteResponse response = searchService.autocomplete("", 10);
        assertThat(response.getSuggestions()).isEmpty();
        assertThat(response.getProducts()).isEmpty();
        assertThat(response.getStores()).isEmpty();
    }
}
