package com.nexora.ai.tool;

import com.nexora.search.dto.StoreSearchResult;
import com.nexora.search.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Approved AI Tool for Store discovery.
 * Queries stores through the search service / database layer safely.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StoreAiTools {

    private final ProductSearchService productSearchService;

    public String getAvailableStores(String category) {
        try {
            List<StoreSearchResult> stores = productSearchService.searchStores(
                    null, category, PageRequest.of(0, 5)).getContent();

            if (stores.isEmpty()) {
                return category != null && !category.isBlank()
                        ? "Currently no open stores found in the '" + category + "' category."
                        : "No open stores are currently listed.";
            }

            StringBuilder sb = new StringBuilder();
            sb.append(category != null && !category.isBlank()
                    ? "Available stores in '" + category + "':\n"
                    : "Available stores on Nexora:\n");

            for (StoreSearchResult s : stores) {
                String cats = (s.getCategories() != null && !s.getCategories().isEmpty())
                        ? String.join(", ", s.getCategories())
                        : "General";
                sb.append(String.format("- %s (Status: %s | Categories: %s)\n",
                        s.getName(), s.getStatus(), cats));
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("getAvailableStores tool error: {}", e.getMessage());
            return "Unable to retrieve store list: " + e.getMessage();
        }
    }
}
