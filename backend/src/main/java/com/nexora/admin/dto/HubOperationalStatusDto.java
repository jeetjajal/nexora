package com.nexora.admin.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HubOperationalStatusDto {
    private Long hubId;
    private String hubName;
    private String code;
    private String city;
    private long activeOrdersCount;
    private int pickersActive;
    private int ridersOnline;
    private double avgPickTimeSeconds;
    private String healthStatus; // OPTIMAL, ELEVATED, CRITICAL_DELAY
}
