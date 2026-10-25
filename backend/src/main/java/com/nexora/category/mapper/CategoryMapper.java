package com.nexora.category.mapper;

import com.nexora.category.dto.CategoryResponse;
import com.nexora.category.entity.Category;

public class CategoryMapper {

    private CategoryMapper() {
    }

    public static CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .imageUrl(category.getImageUrl())
                .build();
    }
}
