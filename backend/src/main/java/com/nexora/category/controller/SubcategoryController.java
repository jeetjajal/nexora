package com.nexora.category.controller;

import com.nexora.category.dto.SubcategoryRequest;
import com.nexora.category.dto.SubcategoryResponse;
import com.nexora.category.service.SubcategoryService;
import com.nexora.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subcategories")
@RequiredArgsConstructor
public class SubcategoryController {

    private final SubcategoryService subcategoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SubcategoryResponse>>> getAllSubcategories(
            @RequestParam(required = false) Long categoryId) {
        List<SubcategoryResponse> list = categoryId != null
                ? subcategoryService.getSubcategoriesByCategory(categoryId)
                : subcategoryService.getAllSubcategories();
        return ResponseEntity.ok(ApiResponse.success("Subcategories fetched successfully", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubcategoryResponse>> getSubcategoryById(@PathVariable Long id) {
        SubcategoryResponse sub = subcategoryService.getSubcategoryById(id);
        return ResponseEntity.ok(ApiResponse.success("Subcategory fetched successfully", sub));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SubcategoryResponse>> createSubcategory(
            @Valid @RequestBody SubcategoryRequest request) {
        SubcategoryResponse response = subcategoryService.createSubcategory(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Subcategory created successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SubcategoryResponse>> updateSubcategory(
            @PathVariable Long id,
            @Valid @RequestBody SubcategoryRequest request) {
        SubcategoryResponse response = subcategoryService.updateSubcategory(id, request);
        return ResponseEntity.ok(ApiResponse.success("Subcategory updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSubcategory(@PathVariable Long id) {
        subcategoryService.deleteSubcategory(id);
        return ResponseEntity.ok(ApiResponse.success("Subcategory deleted successfully", null));
    }
}
