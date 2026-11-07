package com.nexora.wms.repository;

import com.nexora.wms.entity.PickTaskItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PickTaskItemRepository extends JpaRepository<PickTaskItem, Long> {
    List<PickTaskItem> findByPickTaskIdOrderBySequenceIndexAsc(Long pickTaskId);
    Optional<PickTaskItem> findByPickTaskIdAndOrderItemId(Long pickTaskId, Long orderItemId);
}
