package com.nexora.store.repository;

import com.nexora.store.entity.InventoryBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, Long> {

    List<InventoryBatch> findByStoreIdOrderByExpiryDateAsc(Long storeId);

    List<InventoryBatch> findByProductIdAndStoreIdAndIsQuarantinedFalseAndAvailableQuantityGreaterThanOrderByExpiryDateAsc(
            Long productId, Long storeId, int minAvailableQuantity
    );

    List<InventoryBatch> findByStoreIdAndIsQuarantinedTrue(Long storeId);

    List<InventoryBatch> findByStoreIdAndExpiryDateLessThanEqualAndIsQuarantinedFalse(Long storeId, LocalDate expiryCutoff);
}
