package com.nexora.wms.repository;

import com.nexora.wms.entity.PickerProfile;
import com.nexora.wms.enums.PickerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PickerProfileRepository extends JpaRepository<PickerProfile, Long> {
    Optional<PickerProfile> findByUserId(Long userId);
    Optional<PickerProfile> findByEmployeeCode(String employeeCode);
    List<PickerProfile> findByStoreIdAndStatus(Long storeId, PickerStatus status);
    List<PickerProfile> findByStoreId(Long storeId);
}
