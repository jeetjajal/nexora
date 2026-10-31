package com.nexora.mobile.repository;

import com.nexora.mobile.entity.MobileDeviceRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MobileDeviceRegistrationRepository extends JpaRepository<MobileDeviceRegistration, Long> {

    Optional<MobileDeviceRegistration> findByUserIdAndDeviceId(Long userId, String deviceId);

    List<MobileDeviceRegistration> findByUserIdAndIsActiveTrue(Long userId);

    Optional<MobileDeviceRegistration> findByDeviceToken(String deviceToken);
}
