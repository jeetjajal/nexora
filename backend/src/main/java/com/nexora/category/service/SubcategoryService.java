package com.nexora.category.service;

import com.nexora.category.dto.SubcategoryRequest;
import com.nexora.category.dto.SubcategoryResponse;
import com.nexora.category.entity.Category;
import com.nexora.category.entity.Subcategory;
import com.nexora.category.repository.CategoryRepository;
import com.nexora.category.repository.SubcategoryRepository;
import com.nexora.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubcategoryService {

    private final SubcategoryRepository subcategoryRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public SubcategoryResponse createSubcategory(SubcategoryRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        String slug = request.getSlug();
        if (slug == null || slug.isBlank()) {
            slug = request.getName().toLowerCase().replaceAll("[^a-z0-9]+", "-");
        }

        if (subcategoryRepository.existsByCategoryIdAndSlug(request.getCategoryId(), slug)) {
            throw new IllegalArgumentException("Subcategory with slug '" + slug + "' already exists under category " + category.getName());
        }

        Subcategory subcategory = Subcategory.builder()
                .category(category)
                .name(request.getName().trim())
                .slug(slug)
                .description(request.getDescription())
                .imageUrl(request.getImageUrl() != null ? request.getImageUrl() : "📦")
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        Subcategory saved = subcategoryRepository.save(subcategory);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SubcategoryResponse> getAllSubcategories() {
        return subcategoryRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SubcategoryResponse> getSubcategoriesByCategory(Long categoryId) {
        return subcategoryRepository.findByCategoryIdOrderByDisplayOrderAsc(categoryId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SubcategoryResponse getSubcategoryById(Long id) {
        Subcategory sub = subcategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subcategory not found with id: " + id));
        return toResponse(sub);
    }

    @Transactional
    public SubcategoryResponse updateSubcategory(Long id, SubcategoryRequest request) {
        Subcategory sub = subcategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subcategory not found with id: " + id));

        if (request.getCategoryId() != null && !request.getCategoryId().equals(sub.getCategory().getId())) {
            Category newCategory = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));
            sub.setCategory(newCategory);
        }

        if (request.getName() != null) sub.setName(request.getName().trim());
        if (request.getSlug() != null) sub.setSlug(request.getSlug());
        if (request.getDescription() != null) sub.setDescription(request.getDescription());
        if (request.getImageUrl() != null) sub.setImageUrl(request.getImageUrl());
        if (request.getDisplayOrder() != null) sub.setDisplayOrder(request.getDisplayOrder());
        if (request.getActive() != null) sub.setActive(request.getActive());

        Subcategory saved = subcategoryRepository.save(sub);
        return toResponse(saved);
    }

    @Transactional
    public void deleteSubcategory(Long id) {
        Subcategory sub = subcategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subcategory not found with id: " + id));
        subcategoryRepository.delete(sub);
    }

    private SubcategoryResponse toResponse(Subcategory s) {
        return SubcategoryResponse.builder()
                .id(s.getId())
                .categoryId(s.getCategory().getId())
                .categoryName(s.getCategory().getName())
                .name(s.getName())
                .slug(s.getSlug())
                .description(s.getDescription())
                .imageUrl(s.getImageUrl())
                .displayOrder(s.getDisplayOrder())
                .active(s.isActive())
                .build();
    }
}
