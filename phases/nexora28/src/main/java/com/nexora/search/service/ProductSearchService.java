package com.nexora.search.service;

import com.nexora.search.dto.AutocompleteResponse;
import com.nexora.search.dto.ProductSearchResult;
import com.nexora.search.dto.StoreSearchResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface ProductSearchService {

    Page<ProductSearchResult> searchProducts(String query,
                                             String category,
                                             BigDecimal minPrice,
                                             BigDecimal maxPrice,
                                             Boolean available,
                                             Long storeId,
                                             Pageable pageable);

    Page<StoreSearchResult> searchStores(String query,
                                         String category,
                                         Pageable pageable);

    AutocompleteResponse autocomplete(String prefix, int limit);
}
