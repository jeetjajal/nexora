package com.nexora.store.mapper;

import com.nexora.category.entity.Category;
import com.nexora.store.dto.StoreResponse;
import com.nexora.store.entity.Store;

import java.util.stream.Collectors;

public class StoreMapper {

    private StoreMapper() {
    }

    public static StoreResponse toResponse(Store store) {
        return StoreResponse.builder()
                .id(store.getId())
                .name(store.getName())
                .description(store.getDescription())
                .imageUrl(store.getImageUrl())
                .status(store.getStatus().name())
                .openingTime(store.getOpeningTime())
                .closingTime(store.getClosingTime())
                .categories(store.getCategories().stream()
                        .map(Category::getName)
                        .collect(Collectors.toSet()))
                .ownerId(store.getOwner().getId())
                .build();
    }
}
