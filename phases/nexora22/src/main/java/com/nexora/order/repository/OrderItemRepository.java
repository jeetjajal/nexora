package com.nexora.order.repository;

import com.nexora.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Used by ProductService (Phase 4, guard added in Phase 8) to
     * block hard-deleting a product that's referenced by at least one
     * real order — see ProductService.deleteProduct.
     */
    boolean existsByProductId(Long productId);

    /**
     * Phase 20: Aggregates most frequently purchased products across all completed orders.
     */
    @org.springframework.data.jpa.repository.Query("SELECT oi.product.id, COUNT(oi.id) FROM OrderItem oi GROUP BY oi.product.id ORDER BY COUNT(oi.id) DESC")
    java.util.List<Object[]> findTopPurchasedProductIds(org.springframework.data.domain.Pageable pageable);

    /**
     * Phase 20: Finds products that co-occur in the same orders as the given productId.
     */
    @org.springframework.data.jpa.repository.Query("SELECT oi2.product.id, COUNT(oi2.id) FROM OrderItem oi1 JOIN OrderItem oi2 ON oi1.order.id = oi2.order.id WHERE oi1.product.id = :productId AND oi2.product.id <> :productId GROUP BY oi2.product.id ORDER BY COUNT(oi2.id) DESC")
    java.util.List<Object[]> findCoOccurringProductIds(@org.springframework.data.repository.query.Param("productId") Long productId, org.springframework.data.domain.Pageable pageable);
}
