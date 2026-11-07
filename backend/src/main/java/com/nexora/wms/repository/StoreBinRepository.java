package com.nexora.wms.repository;

import com.nexora.wms.entity.StoreBin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreBinRepository extends JpaRepository<StoreBin, Long> {
    Optional<StoreBin> findByFullLocationCode(String fullLocationCode);
    List<StoreBin> findByStoreIdOrderByTraversalSequenceAsc(Long storeId);
    List<StoreBin> findByShelfId(Long shelfId);
}
