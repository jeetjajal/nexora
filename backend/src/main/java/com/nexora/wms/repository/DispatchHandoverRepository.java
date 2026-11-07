package com.nexora.wms.repository;

import com.nexora.wms.entity.DispatchHandover;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DispatchHandoverRepository extends JpaRepository<DispatchHandover, Long> {
    Optional<DispatchHandover> findByOrderId(Long orderId);
    Optional<DispatchHandover> findByBagCode(String bagCode);
    List<DispatchHandover> findByStoreIdOrderByHandoverAtDesc(Long storeId);
}
