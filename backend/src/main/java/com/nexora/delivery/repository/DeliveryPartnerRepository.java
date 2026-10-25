package com.nexora.delivery.repository;

import com.nexora.delivery.entity.DeliveryPartner;
import com.nexora.delivery.entity.DeliveryPartnerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryPartnerRepository extends JpaRepository<DeliveryPartner, Long> {
    Optional<DeliveryPartner> findByUserId(Long userId);
    List<DeliveryPartner> findByStatusAndIsOnlineTrue(DeliveryPartnerStatus status);
}
