package com.nexora.product.controller;

import com.nexora.auth.security.UserPrincipal;
import com.nexora.common.ApiResponse;
import com.nexora.product.dto.ProductRequest;
import com.nexora.product.dto.ProductResponse;
import com.nexora.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Product creation is nested under a store
 * (POST /stores/{storeId}/products) since a product can't exist
 * without one — this also lets ProductService immediately verify the
 * caller owns THAT store before anything is created.
 *
 * Reads (browsing) are open to any authenticated role; mutations
 * require STORE_OWNER or ADMIN at the controller level, with
 * ProductService enforcing the specific-store ownership check.
 */
@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * Sample request (POST /api/v1/stores/3/products):
     * {
     *   "name": "Margherita Pizza",
     *   "description": "Fresh mozzarella, tomato sauce and herbs.",
     *   "price": 249.00,
     *   "discount": 10.00,
     *   "imageUrl": "https://example.com/pizza.jpg",
     *   "categoryNames": ["Pizza"],
     *   "initialStock": 20
     * }
     */
    @PostMapping("/api/v1/stores/{storeId}/products")
    @PreAuthorize("hasAnyRole('STORE_OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @PathVariable Long storeId,
            @Valid @RequestBody ProductRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        ProductResponse response = productService.createProduct(storeId, request, principal);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", response));
    }

    /**
     * Products for a specific store, e.g. GET /api/v1/stores/3/products?page=0&size=10
     */
    @GetMapping("/api/v1/stores/{storeId}/products")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getProductsByStore(
            @PathVariable Long storeId,
            @PageableDefault(size = 10) Pageable pageable) {

        Page<ProductResponse> response = productService.getProductsByStore(storeId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Store products fetched successfully", response));
    }

    /**
     * PHASE 4: platform-wide product browsing, unfiltered.
     * PHASE 5: now also supports optional search/filter query params,
     * e.g.
     *   GET /api/v1/products?name=pizza&categoryName=Pizza&minPrice=100&maxPrice=300&available=true&sort=price,asc
     *
     * Every filter parameter is optional — omitting all of them
     * behaves exactly as it did in Phase 4 (list everything,
     * paginated). Sorting is handled entirely by Spring Data via the
     * `sort` query param on `pageable` (e.g. `sort=price,desc`),
     * so no extra sort-specific parameter is needed here.
     */
    @GetMapping("/api/v1/products")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAllProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean available,
            @PageableDefault(size = 10) Pageable pageable) {

        Page<ProductResponse> response = productService.searchProducts(
                name, categoryName, storeId, minPrice, maxPrice, available, pageable);

        return ResponseEntity.ok(ApiResponse.success("Products fetched successfully", response));
    }

    @GetMapping("/api/v1/products/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success("Product fetched successfully", response));
    }

    @PutMapping("/api/v1/products/{id}")
    @PreAuthorize("hasAnyRole('STORE_OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        ProductResponse response = productService.updateProduct(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", response));
    }

    /**
     * Soft delete — sets available=false. Preferred over a hard delete
     * in real usage since past orders may still reference this product.
     */
    @PatchMapping("/api/v1/products/{id}/deactivate")
    @PreAuthorize("hasAnyRole('STORE_OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> deactivateProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        ProductResponse response = productService.deactivateProduct(id, principal);
        return ResponseEntity.ok(ApiResponse.success("Product deactivated successfully", response));
    }

    /**
     * A genuine hard delete, offered as its own explicit operation
     * per the Phase 4 scope ("Delete/deactivate product").
     */
    @DeleteMapping("/api/v1/products/{id}")
    @PreAuthorize("hasAnyRole('STORE_OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        productService.deleteProduct(id, principal);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
    }
}
