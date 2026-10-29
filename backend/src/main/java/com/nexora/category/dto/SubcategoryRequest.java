package com.nexora.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class SubcategoryRequest {

    @NotNull(message = "Parent category ID is required")
    private Long categoryId;

    @NotBlank(message = "Subcategory name cannot be blank")
    private String name;

    private String slug;
    private String description;
    private String imageUrl;
    private Integer displayOrder;
    private Boolean active;
}
