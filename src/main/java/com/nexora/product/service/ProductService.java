package com.nexora.product.service;

import com.nexora.auth.security.UserPrincipal;
import com.nexora.category.entity.Category;
import com.nexora.category.repository.CategoryRepository;
import com.nexora.exception.ForbiddenOperationException;
import com.nexora.exception.ResourceNotFoundException;
import com.nexora.inventory.entity.Inventory;
import com.nexora.inventory.repository.InventoryRepository;
import com.nexora.product.dto.ProductRequest;
import com.nexora.product.dto.ProductResponse;
import com.nexora.product.entity.Product;
import com.nexora.product.mapper.ProductMapper;
import com.nexora.product.repository.ProductRepository;
import com.nexora.role.entity.RoleName;
import com.nexora.store.entity.Store;
import com.nexora.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Mirrors StoreService's authorization pattern: @PreAuthorize on the
 * controller checks the caller has SOME valid role (STORE_OWNER or
 * ADMIN); this service checks whether they own the SPECIFIC store the
 * product belongs (or would belong) to. A STORE_OWNER can never
 * create/edit/delete a product under a store they don't own, even
 * though their role alone would pass the controller-level check.
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;

    @Transactional
    public ProductResponse createProduct(Long storeId, ProductRequest request, UserPrincipal caller) {
        Store store = findStoreOrThrow(storeId);
        assertCallerOwnsStoreOrIsAdmin(store, caller);

        Product product = Product.builder()
                .store(store)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .discount(request.getDiscount() != null ? request.getDiscount() : java.math.BigDecimal.ZERO)
                .imageUrl(request.getImageUrl())
                .available(true)
                .categories(resolveCategories(request.getCategoryNames()))
                .build();

        Product savedProduct = productRepository.save(product);

        // Every product needs exactly one Inventory row (Phase 2's
        // one-to-one relationship) — we create it here, at product
        // creation time, rather than leaving products without any
        // stock record. Starts at 0 (out of stock) unless the caller
        // specified an initial quantity.
        int startingStock = request.getInitialStock() != null ? request.getInitialStock() : 0;
        Inventory inventory = Inventory.builder()
                .product(savedProduct)
                .quantity(startingStock)
                .build();
        inventoryRepository.save(inventory);

        return ProductMapper.toResponse(savedProduct);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(ProductMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByStore(Long storeId, Pageable pageable) {
        // Confirms the store actually exists so a bad storeId returns
        // a clean 404 instead of silently returning an empty page.
        findStoreOrThrow(storeId);
        return productRepository.findByStoreId(storeId, pageable).map(ProductMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        return ProductMapper.toResponse(findProductOrThrow(id));
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request, UserPrincipal caller) {
        Product product = findProductOrThrow(id);
        assertCallerOwnsStoreOrIsAdmin(product.getStore(), caller);

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setDiscount(request.getDiscount() != null ? request.getDiscount() : java.math.BigDecimal.ZERO);
        product.setImageUrl(request.getImageUrl());
        product.setCategories(resolveCategories(request.getCategoryNames()));

        Product saved = productRepository.save(product);
        return ProductMapper.toResponse(saved);
    }

    /**
     * "Deactivate" (soft delete): sets available=false rather than
     * removing the row. This is what customers browsing products
     * should stop seeing, while order history / past order items
     * referencing this product (Phase 8 onward) remain intact.
     */
    @Transactional
    public ProductResponse deactivateProduct(Long id, UserPrincipal caller) {
        Product product = findProductOrThrow(id);
        assertCallerOwnsStoreOrIsAdmin(product.getStore(), caller);

        product.setAvailable(false);

        Product saved = productRepository.save(product);
        return ProductMapper.toResponse(saved);
    }

    /**
     * A genuine hard delete — removes the Product row (and, via the
     * database foreign key on Inventory.product_id, would fail if
     * Inventory isn't cleaned up first, so we remove that explicitly).
     * Prefer deactivateProduct() in real usage; this exists to satisfy
     * "delete" as its own distinct operation per the Phase 4 scope.
     */
    @Transactional
    public void deleteProduct(Long id, UserPrincipal caller) {
        Product product = findProductOrThrow(id);
        assertCallerOwnsStoreOrIsAdmin(product.getStore(), caller);

        inventoryRepository.findByProductId(product.getId())
                .ifPresent(inventoryRepository::delete);

        productRepository.delete(product);
    }

    // ------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------

    private Store findStoreOrThrow(Long storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with id: " + storeId));
    }

    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    private void assertCallerOwnsStoreOrIsAdmin(Store store, UserPrincipal caller) {
        boolean isAdmin = caller.getUser().getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ADMIN);

        if (isAdmin) {
            return;
        }

        if (!store.getOwner().getId().equals(caller.getId())) {
            throw new ForbiddenOperationException(
                    "You do not have permission to manage products for a store you do not own.");
        }
    }

    private Set<Category> resolveCategories(Set<String> categoryNames) {
        if (categoryNames == null || categoryNames.isEmpty()) {
            return new HashSet<>();
        }

        Set<Category> resolved = new HashSet<>();
        for (String name : categoryNames) {
            Category category = categoryRepository.findByName(name)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Category not found: " + name + " (an ADMIN must create it first)"));
            resolved.add(category);
        }
        return resolved;
    }
}
