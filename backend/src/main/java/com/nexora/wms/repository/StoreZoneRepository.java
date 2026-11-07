package com.nexora.wms.repository;

import com.nexora.wms.entity.StoreZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreZoneRepository extends JpaRepository<StoreZone, Long> {
    List<StoreZone> findByStoreIdAndIsActiveTrue(Long storeId);
    Optional<StoreZone> findByStoreIdAndZoneCode(Long storeId, String zoneCode);
}
