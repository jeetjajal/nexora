package com.nexora.mobile.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceRegistrationResponse {

    private Long id;
    private Long userId;
    private String deviceId;
    private String platform;
    private String appVersion;
    private String pushProvider;
    private Boolean isActive;
    private LocalDateTime registeredAt;
}
