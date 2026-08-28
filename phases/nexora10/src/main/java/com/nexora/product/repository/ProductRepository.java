package com.nexora.product.repository;

import com.nexora.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * PHASE 5 ADDITION: extends JpaSpecificationExecutor<Product>, which
 * adds findAll(Specification<Product>, Pageable) on top of everything
 * JpaRepository already provided. This is what lets ProductService
 * combine an arbitrary set of optional filters (name contains,
 * category, price range, availability, store) into a single dynamic
 * query, instead of writing a separate derived-query method for every
 * possible combination of filters. Nothing about the existing
 * Phase 2/4 methods below changed.
 */
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    List<Product> findByStoreId(Long storeId);

    /**
     * Paginated version used by ProductController's
     * GET /api/v1/stores/{storeId}/products listing.
     */
    Page<Product> findByStoreId(Long storeId, Pageable pageable);

    /**
     * Used by CategoryService to check "is any product still using
     * this category?" before allowing a delete.
     */
    boolean existsByCategoriesId(Long categoryId);
}
