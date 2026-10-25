package com.nexora.phase18;

import com.nexora.category.entity.Category;
import com.nexora.product.entity.Product;
import com.nexora.product.repository.ProductRepository;
import com.nexora.search.document.ProductDocument;
import com.nexora.search.document.StoreDocument;
import com.nexora.search.dto.SearchSyncResponse;
import com.nexora.search.repository.ProductSearchRepository;
import com.nexora.search.repository.StoreSearchRepository;
import com.nexora.search.service.SearchSyncServiceImpl;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchSyncServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private ProductSearchRepository productSearchRepository;

    @Mock
    private StoreSearchRepository storeSearchRepository;

    @InjectMocks
    private SearchSyncServiceImpl searchSyncService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(searchSyncService, "productSearchRepository", productSearchRepository);
        ReflectionTestUtils.setField(searchSyncService, "storeSearchRepository", storeSearchRepository);
    }

    @Test
    @DisplayName("syncAll: bulk indexes all products and stores from MySQL into Elasticsearch")
    void syncAll_Success() {
        Store store = Store.builder()
                .id(1L)
                .name("Taco Bell")
                .description("Mexican Fast Food")
                .status(StoreStatus.OPEN)
                .categories(Set.of(Category.builder().id(1L).name("Mexican").build()))
                .build();

        Product product = Product.builder()
                .id(100L)
                .name("Crunchy Taco")
                .description("Crispy taco shell")
                .price(BigDecimal.valueOf(99.0))
                .store(store)
                .categories(Set.of(Category.builder().id(1L).name("Mexican").build()))
                .available(true)
                .build();

        when(productRepository.findAll()).thenReturn(List.of(product));
        when(storeRepository.findAll()).thenReturn(List.of(store));

        SearchSyncResponse response = searchSyncService.syncAll();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getProductsSynced()).isEqualTo(1L);
        assertThat(response.getStoresSynced()).isEqualTo(1L);
        assertThat(response.getDurationMs()).isGreaterThanOrEqualTo(0);

        verify(productSearchRepository).saveAll(anyList());
        verify(storeSearchRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("syncProduct: saves single product document to Elasticsearch")
    void syncProduct_Success() {
        Product product = Product.builder()
                .id(100L)
                .name("Crunchy Taco")
                .price(BigDecimal.valueOf(99.0))
                .available(true)
                .build();

        when(productRepository.findById(100L)).thenReturn(Optional.of(product));

        searchSyncService.syncProduct(100L);

        verify(productSearchRepository).save(any(ProductDocument.class));
    }

    @Test
    @DisplayName("deleteProduct: removes product document from Elasticsearch by id")
    void deleteProduct_Success() {
        searchSyncService.deleteProduct(100L);

        verify(productSearchRepository).deleteById("prod-100");
    }

    @Test
    @DisplayName("syncStore: saves single store document to Elasticsearch")
    void syncStore_Success() {
        Store store = Store.builder()
                .id(1L)
                .name("Taco Bell")
                .status(StoreStatus.OPEN)
                .build();

        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));

        searchSyncService.syncStore(1L);

        verify(storeSearchRepository).save(any(StoreDocument.class));
    }

    @Test
    @DisplayName("deleteStore: removes store document from Elasticsearch by id")
    void deleteStore_Success() {
        searchSyncService.deleteStore(1L);

        verify(storeSearchRepository).deleteById("store-1");
    }

    @Test
    @DisplayName("syncAll: returns SKIPPED when repositories are inactive/null")
    void syncAll_RepositoriesNull() {
        ReflectionTestUtils.setField(searchSyncService, "productSearchRepository", null);

        SearchSyncResponse response = searchSyncService.syncAll();

        assertThat(response.getStatus()).isEqualTo("SKIPPED");
        assertThat(response.getProductsSynced()).isZero();
    }
}
