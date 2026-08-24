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
import com.nexora.product.exception.InvalidProductDataException;
import com.nexora.product.mapper.ProductMapper;
import com.nexora.product.repository.ProductRepository;
import com.nexora.product.spec.ProductSpecifications;
import com.nexora.role.entity.RoleName;
import com.nexora.store.entity.Store;
import com.nexora.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

        BigDecimal discount = request.getDiscount() != null ? request.getDiscount() : BigDecimal.ZERO;
        assertDiscountDoesNotExceedPrice(request.getPrice(), discount);

        Product product = Product.builder()
                .store(store)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .discount(discount)
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

    /**
     * PHASE 4 (unchanged): plain "list everything, paginated" —
     * still used wherever no filters are needed. Internally now just
     * calls searchProducts() with every filter left null, so both
     * methods share exactly one query-building code path.
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return searchProducts(null, null, null, null, null, null, pageable);
    }

    /**
     * PHASE 5 ADDITION: customer-facing search/filter/sort.
     *
     * Every parameter is OPTIONAL (nullable) — the caller (ProductController)
     * passes whatever the customer actually specified as query params
     * and leaves the rest null. Sorting isn't handled here at all: it
     * rides on the `pageable` parameter's Sort, which Spring Data
     * already knows how to translate into an ORDER BY clause (e.g.
     * ?sort=price,asc) — no custom code needed for that part.
     *
     * @param name          case-insensitive substring match on product name
     * @param categoryName  exact match (case-insensitive) on an attached category's name
     * @param storeId       restrict to a single store
     * @param minPrice      inclusive lower bound on price
     * @param maxPrice      inclusive upper bound on price
     * @param available     true = only in-stock-flagged products, false = only unavailable ones
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(
            String name,
            String categoryName,
            Long storeId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean available,
            Pageable pageable) {

        Specification<Product> spec = Specification
                .where(ProductSpecifications.nameContains(name))
                .and(ProductSpecifications.hasCategoryName(categoryName))
                .and(ProductSpecifications.belongsToStore(storeId))
                .and(ProductSpecifications.priceGreaterThanOrEqual(minPrice))
                .and(ProductSpecifications.priceLessThanOrEqual(maxPrice))
                .and(ProductSpecifications.isAvailable(available));

        return productRepository.findAll(spec, pageable).map(ProductMapper::toResponse);
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

        BigDecimal discount = request.getDiscount() != null ? request.getDiscount() : BigDecimal.ZERO;
        assertDiscountDoesNotExceedPrice(request.getPrice(), discount);

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setDiscount(discount);
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

    private void assertDiscountDoesNotExceedPrice(BigDecimal price, BigDecimal discount) {
        if (price != null && discount != null && discount.compareTo(price) > 0) {
            throw new InvalidProductDataException(
                    "Discount (" + discount + ") cannot be greater than price (" + price + ")");
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
