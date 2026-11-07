package com.nexora.wms.repository;

import com.nexora.wms.entity.PickingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PickingEventRepository extends JpaRepository<PickingEvent, Long> {
    List<PickingEvent> findByPickTaskIdOrderByCreatedAtAsc(Long pickTaskId);
}
