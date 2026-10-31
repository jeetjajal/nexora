package com.nexora.delivery.repository;

import com.nexora.delivery.entity.DeliveryBatchOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryBatchOrderRepository extends JpaRepository<DeliveryBatchOrder, Long> {

    List<DeliveryBatchOrder> findByBatchIdOrderBySequenceIndexAsc(Long batchId);

    Optional<DeliveryBatchOrder> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);
}
