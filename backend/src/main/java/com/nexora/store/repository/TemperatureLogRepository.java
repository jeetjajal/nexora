package com.nexora.store.repository;

import com.nexora.store.entity.TemperatureLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemperatureLogRepository extends JpaRepository<TemperatureLog, Long> {

    List<TemperatureLog> findByStoreIdOrderByRecordedAtDesc(Long storeId);

    List<TemperatureLog> findTop20ByStoreIdOrderByRecordedAtDesc(Long storeId);

    List<TemperatureLog> findByStoreIdAndBreachDetectedTrueOrderByRecordedAtDesc(Long storeId);
}
