package com.nexora.order.repository;

import com.nexora.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserId(Long userId, Pageable pageable);

    Page<Order> findByStoreId(Long storeId, Pageable pageable);

    java.util.List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
}
