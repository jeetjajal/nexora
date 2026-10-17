package com.nexora.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSearchResult {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal discount;
    private String imageUrl;
    private boolean available;
    private Long storeId;
    private String storeName;
    private List<String> categories;
    private String source; // "ELASTICSEARCH" or "DATABASE_FALLBACK"
}
