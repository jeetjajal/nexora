package com.nexora.delivery.repository;

import com.nexora.delivery.entity.DeliveryAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nexora.delivery.entity.AssignmentStatus;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, Long> {
    Optional<DeliveryAssignment> findByOrderId(Long orderId);
    List<DeliveryAssignment> findByDeliveryPartnerIdAndStatus(Long deliveryPartnerId, AssignmentStatus status);
}
