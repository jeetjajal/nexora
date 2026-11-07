package com.nexora.wms.repository;

import com.nexora.wms.entity.StoreShelf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreShelfRepository extends JpaRepository<StoreShelf, Long> {
    List<StoreShelf> findByRackIdOrderByShelfLevelAsc(Long rackId);
    Optional<StoreShelf> findByRackIdAndShelfCode(Long rackId, String shelfCode);
}
