package com.nexora.wms.repository;

import com.nexora.wms.entity.StoreAisle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreAisleRepository extends JpaRepository<StoreAisle, Long> {
    List<StoreAisle> findByStoreIdOrderByAisleNumberAsc(Long storeId);
    List<StoreAisle> findByZoneIdOrderByAisleNumberAsc(Long zoneId);
    Optional<StoreAisle> findByStoreIdAndAisleCode(Long storeId, String aisleCode);
}
