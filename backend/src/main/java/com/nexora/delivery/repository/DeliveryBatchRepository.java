package com.nexora.delivery.repository;

import com.nexora.delivery.entity.DeliveryBatch;
import com.nexora.delivery.entity.DeliveryBatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryBatchRepository extends JpaRepository<DeliveryBatch, Long> {

    Optional<DeliveryBatch> findByBatchCode(String batchCode);

    List<DeliveryBatch> findByStoreId(Long storeId);

    List<DeliveryBatch> findByStoreIdAndStatus(Long storeId, DeliveryBatchStatus status);

    List<DeliveryBatch> findByStatus(DeliveryBatchStatus status);

    List<DeliveryBatch> findByDeliveryPartnerIdAndStatus(Long deliveryPartnerId, DeliveryBatchStatus status);

    List<DeliveryBatch> findAllByOrderByCreatedAtDesc();
}
