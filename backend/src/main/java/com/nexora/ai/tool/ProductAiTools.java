package com.nexora.ai.tool;

import com.nexora.search.dto.ProductSearchResult;
import com.nexora.search.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Approved AI Tool for Product and food search.
 * Leverages Phase 18 full-text / category search safely without direct SQL.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductAiTools {

    private final ProductSearchService productSearchService;

    public String searchProducts(String query, String category, Double maxPrice) {
        try {
            BigDecimal max = maxPrice != null && maxPrice > 0 ? BigDecimal.valueOf(maxPrice) : null;
            List<ProductSearchResult> products = productSearchService.searchProducts(
                    query, category, null, max, true, null, PageRequest.of(0, 5)).getContent();

            if (products.isEmpty()) {
                return "No available products found matching your search criteria.";
            }

            StringBuilder sb = new StringBuilder("Here is what I found on Nexora:\n");
            for (ProductSearchResult p : products) {
                sb.append(String.format("- %s (₹%.2f) from '%s' | Available: %s\n",
                        p.getName(),
                        p.getPrice(),
                        p.getStoreName() != null ? p.getStoreName() : "Store #" + p.getStoreId(),
                        p.isAvailable() ? "In Stock" : "Out of Stock"));
                if (p.getDescription() != null && !p.getDescription().isBlank()) {
                    sb.append("  ").append(p.getDescription()).append("\n");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("searchProducts tool error: {}", e.getMessage());
            return "Unable to search products: " + e.getMessage();
        }
    }
}
