package com.nexora.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiderSelfieVerificationRequest {
    private String imageBase64;
    private String livenessProof;
    private Double latitude;
    private Double longitude;
}
