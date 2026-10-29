package com.nexora.category.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubcategoryResponse {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private Integer displayOrder;
    private boolean active;
}
