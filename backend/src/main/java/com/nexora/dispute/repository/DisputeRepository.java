package com.nexora.dispute.repository;

import com.nexora.dispute.entity.Dispute;
import com.nexora.dispute.entity.DisputeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {
    List<Dispute> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<Dispute> findByStatusOrderByCreatedAtDesc(DisputeStatus status);
    List<Dispute> findAllByOrderByCreatedAtDesc();
    Optional<Dispute> findByOrderId(Long orderId);
    boolean existsByOrderIdAndStatusIn(Long orderId, java.util.Collection<DisputeStatus> statuses);
}
