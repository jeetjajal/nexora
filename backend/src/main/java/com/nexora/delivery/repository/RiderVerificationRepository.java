package com.nexora.delivery.repository;

import com.nexora.delivery.entity.RiderVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RiderVerificationRepository extends JpaRepository<RiderVerification, Long> {

    Optional<RiderVerification> findTopByDeliveryPartnerIdAndShiftDateOrderByCreatedAtDesc(
            Long deliveryPartnerId, LocalDate shiftDate
    );

    List<RiderVerification> findByShiftDateAndStatus(LocalDate shiftDate, String status);

    List<RiderVerification> findByDeliveryPartnerIdOrderByCreatedAtDesc(Long deliveryPartnerId);
}
