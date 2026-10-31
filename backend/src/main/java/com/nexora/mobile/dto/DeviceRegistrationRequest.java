package com.nexora.mobile.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceRegistrationRequest {

    @NotBlank(message = "Device token is required")
    private String deviceToken;

    @NotBlank(message = "Device ID is required")
    private String deviceId;

    @NotBlank(message = "Platform is required (ANDROID, IOS, WEB_PWA)")
    private String platform;

    @NotBlank(message = "App version is required")
    private String appVersion;

    private String pushProvider; // Default: FCM
}
