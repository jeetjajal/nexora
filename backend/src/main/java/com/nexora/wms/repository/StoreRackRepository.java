package com.nexora.wms.repository;

import com.nexora.wms.entity.StoreRack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRackRepository extends JpaRepository<StoreRack, Long> {
    List<StoreRack> findByAisleIdOrderByRackNumberAsc(Long aisleId);
    Optional<StoreRack> findByAisleIdAndRackCode(Long aisleId, String rackCode);
}
