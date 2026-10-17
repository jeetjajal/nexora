package com.nexora.phase19;

import com.nexora.ai.tool.ProductAiTools;
import com.nexora.ai.tool.StoreAiTools;
import com.nexora.search.dto.ProductSearchResult;
import com.nexora.search.dto.StoreSearchResult;
import com.nexora.search.service.ProductSearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreAndProductAiToolsTest {

    @Mock
    private ProductSearchService productSearchService;

    @InjectMocks
    private StoreAiTools storeAiTools;

    @InjectMocks
    private ProductAiTools productAiTools;

    @Test
    @DisplayName("getAvailableStores: returns list of open stores")
    void getAvailableStores_Success() {
        StoreSearchResult store = StoreSearchResult.builder()
                .id(1L)
                .name("Pizza Corner")
                .status("OPEN")
                .categories(List.of("Pizza", "Italian"))
                .build();

        when(productSearchService.searchStores(eq(null), eq("Pizza"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(store)));

        String result = storeAiTools.getAvailableStores("Pizza");

        assertThat(result).contains("Available stores in 'Pizza'");
        assertThat(result).contains("Pizza Corner");
        assertThat(result).contains("OPEN");
    }

    @Test
    @DisplayName("searchProducts: finds matching food with price budget")
    void searchProducts_Success() {
        ProductSearchResult item = ProductSearchResult.builder()
                .id(10L)
                .name("Veggie Burger")
                .description("Crispy patty with fresh lettuce")
                .price(BigDecimal.valueOf(149.0))
                .storeName("Burger King")
                .available(true)
                .build();

        when(productSearchService.searchProducts(eq("burger"), eq(null), eq(null), eq(BigDecimal.valueOf(200.0)), eq(true), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item)));

        String result = productAiTools.searchProducts("burger", null, 200.0);

        assertThat(result).contains("Here is what I found on Nexora");
        assertThat(result).contains("Veggie Burger");
        assertThat(result).contains("149.00");
    }
}
