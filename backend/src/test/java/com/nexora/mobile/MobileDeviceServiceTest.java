package com.nexora.mobile;

import com.nexora.mobile.dto.DeviceRegistrationRequest;
import com.nexora.mobile.dto.DeviceRegistrationResponse;
import com.nexora.mobile.entity.MobileDeviceRegistration;
import com.nexora.mobile.repository.MobileDeviceRegistrationRepository;
import com.nexora.mobile.service.MobileDeviceService;
import com.nexora.user.entity.User;
import com.nexora.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 6 - MobileDeviceService Native Registration Tests")
class MobileDeviceServiceTest {

    @Mock
    private MobileDeviceRegistrationRepository deviceRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MobileDeviceService deviceService;

    private User testUser;
    private MobileDeviceRegistration existingDevice;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(50L).name("Rider Ramesh").phone("9876543210").build();

        existingDevice = MobileDeviceRegistration.builder()
                .id(1L)
                .user(testUser)
                .deviceId("uuid-android-9874")
                .deviceToken("fcm-token-old")
                .platform("ANDROID")
                .appVersion("2.9.0")
                .pushProvider("FCM")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Register new mobile device creates fresh registration record")
    void testRegisterNewDevice_Success() {
        when(userRepository.findById(50L)).thenReturn(Optional.of(testUser));
        when(deviceRepository.findByUserIdAndDeviceId(50L, "uuid-ios-1234")).thenReturn(Optional.empty());
        when(deviceRepository.save(any(MobileDeviceRegistration.class))).thenAnswer(i -> {
            MobileDeviceRegistration d = i.getArgument(0);
            d.setId(10L);
            return d;
        });

        DeviceRegistrationRequest req = DeviceRegistrationRequest.builder()
                .deviceId("uuid-ios-1234")
                .deviceToken("apns-token-xyz")
                .platform("IOS")
                .appVersion("3.0.0")
                .pushProvider("APNS")
                .build();

        DeviceRegistrationResponse resp = deviceService.registerDevice(50L, req);

        assertNotNull(resp);
        assertEquals("IOS", resp.getPlatform());
        assertEquals("3.0.0", resp.getAppVersion());
        assertEquals("APNS", resp.getPushProvider());
        assertTrue(resp.getIsActive());
        verify(deviceRepository, times(1)).save(any(MobileDeviceRegistration.class));
    }

    @Test
    @DisplayName("Register existing device updates token and app version without duplication")
    void testRegisterExistingDevice_UpdatesToken() {
        when(userRepository.findById(50L)).thenReturn(Optional.of(testUser));
        when(deviceRepository.findByUserIdAndDeviceId(50L, "uuid-android-9874")).thenReturn(Optional.of(existingDevice));
        when(deviceRepository.save(any(MobileDeviceRegistration.class))).thenAnswer(i -> i.getArgument(0));

        DeviceRegistrationRequest req = DeviceRegistrationRequest.builder()
                .deviceId("uuid-android-9874")
                .deviceToken("fcm-token-refreshed-v3")
                .platform("ANDROID")
                .appVersion("3.0.0")
                .pushProvider("FCM")
                .build();

        DeviceRegistrationResponse resp = deviceService.registerDevice(50L, req);

        assertNotNull(resp);
        assertEquals(1L, resp.getId());
        assertEquals("3.0.0", resp.getAppVersion());
        assertEquals("ANDROID", resp.getPlatform());
        assertTrue(resp.getIsActive());
        verify(deviceRepository, times(1)).save(existingDevice);
    }

    @Test
    @DisplayName("Fetch active devices returns only active registered hardware")
    void testGetUserActiveDevices() {
        when(deviceRepository.findByUserIdAndIsActiveTrue(50L)).thenReturn(List.of(existingDevice));

        List<DeviceRegistrationResponse> devices = deviceService.getUserActiveDevices(50L);

        assertEquals(1, devices.size());
        assertEquals("uuid-android-9874", devices.get(0).getDeviceId());
    }

    @Test
    @DisplayName("Deactivate device marks isActive as false")
    void testDeactivateDevice() {
        when(deviceRepository.findByUserIdAndDeviceId(50L, "uuid-android-9874")).thenReturn(Optional.of(existingDevice));
        when(deviceRepository.save(any(MobileDeviceRegistration.class))).thenAnswer(i -> i.getArgument(0));

        deviceService.deactivateDevice(50L, "uuid-android-9874");

        assertFalse(existingDevice.getIsActive());
        verify(deviceRepository, times(1)).save(existingDevice);
    }
}
