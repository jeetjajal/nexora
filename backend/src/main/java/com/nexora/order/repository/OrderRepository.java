package com.nexora.order.repository;

import com.nexora.order.entity.Order;
import com.nexora.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserId(Long userId, Pageable pageable);

    Page<Order> findByStoreId(Long storeId, Pageable pageable);

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Order> findByStoreIdOrderByCreatedAtDesc(Long storeId);

    List<Order> findByStoreIdAndStatus(Long storeId, OrderStatus status);

    List<Order> findByStoreIdAndStatusInOrderByCreatedAtAsc(Long storeId, Collection<OrderStatus> statuses);

    long countByStoreIdAndStatus(Long storeId, OrderStatus status);
}
