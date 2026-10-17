package com.nexora.product.mapper;

import com.nexora.category.entity.Category;
import com.nexora.product.dto.ProductResponse;
import com.nexora.product.entity.Product;

import java.util.stream.Collectors;

public class ProductMapper {

    private ProductMapper() {
    }

    public static ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .discount(product.getDiscount())
                .imageUrl(product.getImageUrl())
                .available(product.isAvailable())
                .categories(product.getCategories().stream()
                        .map(Category::getName)
                        .collect(Collectors.toSet()))
                .storeId(product.getStore().getId())
                .build();
    }
}
