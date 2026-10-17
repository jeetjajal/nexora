package com.nexora.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreSearchResult {
    private Long id;
    private String name;
    private String description;
    private String status;
    private String imageUrl;
    private List<String> categories;
    private String source; // "ELASTICSEARCH" or "DATABASE_FALLBACK"
}
