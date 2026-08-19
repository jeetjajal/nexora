package com.nexora.product.repository;

import com.nexora.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

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
