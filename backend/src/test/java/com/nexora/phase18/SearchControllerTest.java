package com.nexora.phase18;

import com.nexora.search.controller.SearchController;
import com.nexora.search.dto.AutocompleteResponse;
import com.nexora.search.dto.ProductSearchResult;
import com.nexora.search.dto.SearchSyncResponse;
import com.nexora.search.dto.StoreSearchResult;
import com.nexora.search.service.ProductSearchService;
import com.nexora.search.service.SearchSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SearchControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductSearchService productSearchService;

    @Mock
    private SearchSyncService searchSyncService;

    @InjectMocks
    private SearchController searchController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(searchController).build();
    }

    @Test
    @DisplayName("GET /api/v1/search/products: returns 200 with paginated product search results")
    void searchProducts_Endpoint() throws Exception {
        ProductSearchResult item = ProductSearchResult.builder()
                .id(1L)
                .name("Margherita Pizza")
                .description("Cheesy delicious pizza")
                .price(BigDecimal.valueOf(299.0))
                .available(true)
                .storeId(5L)
                .storeName("Domino's")
                .categories(List.of("Pizza", "Italian"))
                .source("ELASTICSEARCH")
                .build();

        Page<ProductSearchResult> page = new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1);
        when(productSearchService.searchProducts(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/search/products")
                        .param("query", "pizza")
                        .param("category", "Italian")
                        .param("minPrice", "100")
                        .param("maxPrice", "500")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Margherita Pizza"))
                .andExpect(jsonPath("$.content[0].source").value("ELASTICSEARCH"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/search/stores: returns 200 with paginated store search results")
    void searchStores_Endpoint() throws Exception {
        StoreSearchResult item = StoreSearchResult.builder()
                .id(5L)
                .name("Domino's")
                .description("Fast delivery pizza")
                .status("OPEN")
                .categories(List.of("Pizza"))
                .source("ELASTICSEARCH")
                .build();

        Page<StoreSearchResult> page = new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1);
        when(productSearchService.searchStores(any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/search/stores")
                        .param("query", "domino")
                        .param("category", "Pizza")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(5))
                .andExpect(jsonPath("$.content[0].name").value("Domino's"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/search/autocomplete: returns 200 with suggestions and top items")
    void autocomplete_Endpoint() throws Exception {
        AutocompleteResponse response = AutocompleteResponse.builder()
                .query("piz")
                .suggestions(List.of("Pizza", "Pizza Hut"))
                .products(List.of(ProductSearchResult.builder().id(1L).name("Pizza").build()))
                .stores(List.of(StoreSearchResult.builder().id(10L).name("Pizza Hut").build()))
                .build();

        when(productSearchService.autocomplete(eq("piz"), anyInt()))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/search/autocomplete")
                        .param("prefix", "piz")
                        .param("limit", "5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("piz"))
                .andExpect(jsonPath("$.suggestions[0]").value("Pizza"))
                .andExpect(jsonPath("$.products[0].name").value("Pizza"))
                .andExpect(jsonPath("$.stores[0].name").value("Pizza Hut"));
    }

    @Test
    @DisplayName("POST /api/v1/search/sync: returns 200 with sync metrics")
    void syncSearchIndex_Endpoint() throws Exception {
        SearchSyncResponse response = SearchSyncResponse.builder()
                .status("SUCCESS")
                .message("Sync completed")
                .productsSynced(50)
                .storesSynced(10)
                .durationMs(120)
                .build();

        when(searchSyncService.syncAll()).thenReturn(response);

        mockMvc.perform(post("/api/v1/search/sync")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.productsSynced").value(50))
                .andExpect(jsonPath("$.storesSynced").value(10));
    }
}
