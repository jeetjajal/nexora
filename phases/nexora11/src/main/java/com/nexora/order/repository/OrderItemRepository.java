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
}
