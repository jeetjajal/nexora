package com.nexora.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiderVerificationResponse {
    private Long id;
    private Long deliveryPartnerId;
    private String riderName;
    private LocalDate shiftDate;
    private String verificationType;
    private String status; // PENDING, APPROVED, REJECTED, MANUAL_REVIEW
    private Double livenessScore;
    private Double matchScore;
    private Boolean isShiftActive;
    private String reviewNotes;
    private LocalDateTime verifiedAt;
    private String message;
}
