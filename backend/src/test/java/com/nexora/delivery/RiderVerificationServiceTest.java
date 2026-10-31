package com.nexora.delivery;

import com.nexora.delivery.dto.RiderManualOverrideRequest;
import com.nexora.delivery.dto.RiderSelfieVerificationRequest;
import com.nexora.delivery.dto.RiderVerificationResponse;
import com.nexora.delivery.entity.DeliveryPartner;
import com.nexora.delivery.entity.RiderVerification;
import com.nexora.delivery.repository.DeliveryPartnerRepository;
import com.nexora.delivery.repository.RiderVerificationRepository;
import com.nexora.delivery.service.RiderVerificationService;
import com.nexora.user.entity.User;
import com.nexora.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 5 - RiderVerificationService Shift KYC Tests")
class RiderVerificationServiceTest {

    @Mock
    private RiderVerificationRepository verificationRepository;

    @Mock
    private DeliveryPartnerRepository partnerRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RiderVerificationService verificationService;

    private DeliveryPartner testPartner;
    private User testUser;
    private User supervisorUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(101L).name("Ramesh Patel").phone("9876543210").build();
        testPartner = DeliveryPartner.builder().id(501L).user(testUser).build();
        supervisorUser = User.builder().id(201L).name("Store Manager Dilip").phone("9876543299").build();
    }

    @Test
    @DisplayName("Valid shift-start selfie passes biometric verification with > 0.85 liveness")
    void testVerifyShiftSelfieApproved() {
        when(partnerRepository.findByUserId(101L)).thenReturn(Optional.of(testPartner));
        when(verificationRepository.save(any(RiderVerification.class))).thenAnswer(i -> {
            RiderVerification v = i.getArgument(0);
            v.setId(99L);
            return v;
        });

        RiderSelfieVerificationRequest req = RiderSelfieVerificationRequest.builder()
                .imageBase64("data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP...")
                .latitude(23.0373)
                .longitude(72.5118)
                .build();

        RiderVerificationResponse response = verificationService.verifyShiftSelfie(101L, req);

        assertNotNull(response);
        assertEquals("APPROVED", response.getStatus());
        assertTrue(response.getIsShiftActive());
        assertTrue(response.getLivenessScore() >= 0.85);
        assertTrue(response.getMatchScore() >= 0.80);

        verify(verificationRepository, times(1)).save(any(RiderVerification.class));
    }

    @Test
    @DisplayName("Anti-spoofing alert flags printed photo / screen reflection as REJECTED")
    void testVerifyShiftSelfieAntiSpoofingAlert() {
        when(partnerRepository.findByUserId(101L)).thenReturn(Optional.of(testPartner));
        when(verificationRepository.save(any(RiderVerification.class))).thenAnswer(i -> {
            RiderVerification v = i.getArgument(0);
            v.setId(99L);
            return v;
        });

        RiderSelfieVerificationRequest req = RiderSelfieVerificationRequest.builder()
                .imageBase64("data:image/jpeg;base64,spoof_test_screen_reflection")
                .latitude(23.0373)
                .longitude(72.5118)
                .build();

        RiderVerificationResponse response = verificationService.verifyShiftSelfie(101L, req);

        assertEquals("REJECTED", response.getStatus());
        assertFalse(response.getIsShiftActive());
        assertTrue(response.getReviewNotes().contains("Anti-spoofing alert"));
    }

    @Test
    @DisplayName("Supervisor manual override approves shift verification with audit trail")
    void testManualSupervisorOverride() {
        when(userRepository.findById(201L)).thenReturn(Optional.of(supervisorUser));
        when(partnerRepository.findById(501L)).thenReturn(Optional.of(testPartner));
        when(verificationRepository.save(any(RiderVerification.class))).thenAnswer(i -> {
            RiderVerification v = i.getArgument(0);
            v.setId(100L);
            return v;
        });

        RiderManualOverrideRequest req = RiderManualOverrideRequest.builder()
                .deliveryPartnerId(501L)
                .reason("Camera hardware failure on rider device")
                .supervisorNotes("Physically verified rider in hub uniform at store gate")
                .build();

        RiderVerificationResponse response = verificationService.manualSupervisorOverride(201L, req);

        assertEquals("APPROVED", response.getStatus());
        assertEquals("MANUAL_SUPERVISOR", response.getVerificationType());
        assertTrue(response.getIsShiftActive());
        assertTrue(response.getReviewNotes().contains("Camera hardware failure"));
    }
}
