package com.nexora.admin.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FleetRiderStatusDto {
    private Long partnerId;
    private Long userId;
    private String name;
    private String phone;
    private String vehicleType;
    private BigDecimal rating;
    private Boolean isOnline;
    private String currentStatus; // AVAILABLE, BUSY, OFFLINE
    private Long activeOrderId;
    private Double currentLat;
    private Double currentLng;
    private BigDecimal todayEarnings;
    private LocalDateTime lastPingTime;
    private Boolean isStaleLocation;
}
