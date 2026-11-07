package com.nexora.wms.repository;

import com.nexora.wms.entity.PickTask;
import com.nexora.wms.enums.PickTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PickTaskRepository extends JpaRepository<PickTask, Long> {
    Optional<PickTask> findByOrderId(Long orderId);
    Optional<PickTask> findByTaskCode(String taskCode);
    List<PickTask> findByStoreIdAndStatus(Long storeId, PickTaskStatus status);
    List<PickTask> findByPickerIdAndStatus(Long pickerId, PickTaskStatus status);
    List<PickTask> findByStoreIdOrderByCreatedAtDesc(Long storeId);
}
