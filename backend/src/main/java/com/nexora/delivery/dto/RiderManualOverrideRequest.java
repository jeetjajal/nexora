package com.nexora.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiderManualOverrideRequest {
    private Long deliveryPartnerId;
    private String reason;
    private String supervisorNotes;
}
