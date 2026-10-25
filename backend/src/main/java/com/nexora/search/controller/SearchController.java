package com.nexora.search.controller;

import com.nexora.search.dto.AutocompleteResponse;
import com.nexora.search.dto.ProductSearchResult;
import com.nexora.search.dto.SearchSyncResponse;
import com.nexora.search.dto.StoreSearchResult;
import com.nexora.search.service.ProductSearchService;
import com.nexora.search.service.SearchSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final ProductSearchService productSearchService;
    private final SearchSyncService searchSyncService;

    @GetMapping("/products")
    public ResponseEntity<Page<ProductSearchResult>> searchProducts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ProductSearchResult> results = productSearchService.searchProducts(
                query, category, minPrice, maxPrice, available, storeId, pageable);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/stores")
    public ResponseEntity<Page<StoreSearchResult>> searchStores(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<StoreSearchResult> results = productSearchService.searchStores(query, category, pageable);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/autocomplete")
    public ResponseEntity<AutocompleteResponse> autocomplete(
            @RequestParam(name = "prefix", required = false) String prefix,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(defaultValue = "10") int limit
    ) {
        String searchTerm = prefix != null ? prefix : q;
        AutocompleteResponse response = productSearchService.autocomplete(searchTerm, limit);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sync")
    public ResponseEntity<SearchSyncResponse> syncSearchIndex() {
        SearchSyncResponse response = searchSyncService.syncAll();
        return ResponseEntity.ok(response);
    }
}
