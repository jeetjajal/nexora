package com.nexora.wms.repository;

import com.nexora.wms.entity.PackingTask;
import com.nexora.wms.enums.PackingTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PackingTaskRepository extends JpaRepository<PackingTask, Long> {
    Optional<PackingTask> findByOrderId(Long orderId);
    Optional<PackingTask> findByBagCode(String bagCode);
    List<PackingTask> findByStoreIdAndStatus(Long storeId, PackingTaskStatus status);
}
