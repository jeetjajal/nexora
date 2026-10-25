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
public class AutocompleteResponse {
    private String query;
    private List<String> suggestions;
    private List<ProductSearchResult> products;
    private List<StoreSearchResult> stores;
}
