package com.nexora.category.service;

import com.nexora.category.dto.CategoryRequest;
import com.nexora.category.dto.CategoryResponse;
import com.nexora.category.entity.Category;
import com.nexora.category.exception.CategoryInUseException;
import com.nexora.category.exception.DuplicateCategoryException;
import com.nexora.category.mapper.CategoryMapper;
import com.nexora.category.repository.CategoryRepository;
import com.nexora.exception.ResourceNotFoundException;
import com.nexora.product.repository.ProductRepository;
import com.nexora.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Categories have NO ownership concept (unlike Store/Product) — they're
 * a shared, platform-wide vocabulary. That's why authorization for
 * mutating endpoints is a simple role check (ADMIN only, enforced via
 * @PreAuthorize on the controller) with no resource-level ownership
 * check needed here, unlike StoreService/ProductService.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        categoryRepository.findByName(request.getName()).ifPresent(existing -> {
            throw new DuplicateCategoryException(request.getName());
        });

        Category category = Category.builder()
                .name(request.getName())
                .imageUrl(request.getImageUrl())
                .build();

        Category saved = categoryRepository.save(category);
        return CategoryMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(CategoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        return CategoryMapper.toResponse(findCategoryOrThrow(id));
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = findCategoryOrThrow(id);

        // If renaming, make sure the new name isn't already taken by
        // a DIFFERENT category.
        categoryRepository.findByName(request.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new DuplicateCategoryException(request.getName());
            }
        });

        category.setName(request.getName());
        category.setImageUrl(request.getImageUrl());

        Category saved = categoryRepository.save(category);
        return CategoryMapper.toResponse(saved);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = findCategoryOrThrow(id);

        // Category is referenced from the OWNING side of the
        // relationship (Store and Product each declare the @JoinTable
        // — see Phase 2). Without checking first, deleting a Category
        // still attached to any store/product would fail at the
        // database level with a raw foreign-key violation. We check
        // explicitly here so the client gets a clean, actionable 409
        // instead of a confusing 500.
        boolean inUseByStore = storeRepository.existsByCategoriesId(id);
        boolean inUseByProduct = productRepository.existsByCategoriesId(id);

        if (inUseByStore || inUseByProduct) {
            throw new CategoryInUseException(category.getName());
        }

        categoryRepository.delete(category);
    }

    private Category findCategoryOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }
}
