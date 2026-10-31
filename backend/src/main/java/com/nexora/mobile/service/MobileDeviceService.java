package com.nexora.mobile.service;

import com.nexora.exception.ResourceNotFoundException;
import com.nexora.mobile.dto.DeviceRegistrationRequest;
import com.nexora.mobile.dto.DeviceRegistrationResponse;
import com.nexora.mobile.entity.MobileDeviceRegistration;
import com.nexora.mobile.repository.MobileDeviceRegistrationRepository;
import com.nexora.user.entity.User;
import com.nexora.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MobileDeviceService {

    private final MobileDeviceRegistrationRepository deviceRepository;
    private final UserRepository userRepository;

    @Transactional
    public DeviceRegistrationResponse registerDevice(Long userId, DeviceRegistrationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        String pushProvider = (request.getPushProvider() != null && !request.getPushProvider().isBlank())
                ? request.getPushProvider().toUpperCase()
                : "FCM";

        MobileDeviceRegistration device = deviceRepository.findByUserIdAndDeviceId(userId, request.getDeviceId())
                .map(existing -> {
                    existing.setDeviceToken(request.getDeviceToken());
                    existing.setPlatform(request.getPlatform().toUpperCase());
                    existing.setAppVersion(request.getAppVersion());
                    existing.setPushProvider(pushProvider);
                    existing.setIsActive(true);
                    log.info("Updated existing mobile device registration for user #{}, deviceId: {}", userId, request.getDeviceId());
                    return existing;
                })
                .orElseGet(() -> {
                    log.info("Registered new mobile device for user #{}, platform: {}, deviceId: {}",
                            userId, request.getPlatform(), request.getDeviceId());
                    return MobileDeviceRegistration.builder()
                            .user(user)
                            .deviceId(request.getDeviceId())
                            .deviceToken(request.getDeviceToken())
                            .platform(request.getPlatform().toUpperCase())
                            .appVersion(request.getAppVersion())
                            .pushProvider(pushProvider)
                            .isActive(true)
                            .build();
                });

        device = deviceRepository.save(device);
        return mapToResponse(device);
    }

    @Transactional(readOnly = true)
    public List<DeviceRegistrationResponse> getUserActiveDevices(Long userId) {
        return deviceRepository.findByUserIdAndIsActiveTrue(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deactivateDevice(Long userId, String deviceId) {
        deviceRepository.findByUserIdAndDeviceId(userId, deviceId).ifPresent(device -> {
            device.setIsActive(false);
            deviceRepository.save(device);
            log.info("Deactivated mobile device for user #{}, deviceId: {}", userId, deviceId);
        });
    }

    private DeviceRegistrationResponse mapToResponse(MobileDeviceRegistration d) {
        return DeviceRegistrationResponse.builder()
                .id(d.getId())
                .userId(d.getUser().getId())
                .deviceId(d.getDeviceId())
                .platform(d.getPlatform())
                .appVersion(d.getAppVersion())
                .pushProvider(d.getPushProvider())
                .isActive(d.getIsActive())
                .registeredAt(d.getCreatedAt() != null ? d.getCreatedAt() : d.getUpdatedAt())
                .build();
    }
}
