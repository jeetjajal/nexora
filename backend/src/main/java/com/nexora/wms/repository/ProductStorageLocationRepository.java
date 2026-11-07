package com.nexora.wms.repository;

import com.nexora.wms.entity.ProductStorageLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductStorageLocationRepository extends JpaRepository<ProductStorageLocation, Long> {
    List<ProductStorageLocation> findByProductIdAndStoreId(Long productId, Long storeId);
    Optional<ProductStorageLocation> findFirstByProductIdAndStoreIdAndIsPrimaryTrue(Long productId, Long storeId);
    List<ProductStorageLocation> findByBinId(Long binId);
}
