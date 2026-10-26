package com.nexora.notification.controller;

import com.nexora.auth.security.UserPrincipal;
import com.nexora.common.ApiResponse;
import com.nexora.notification.dto.NotificationDto;
import com.nexora.notification.entity.NotificationLog;
import com.nexora.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @PostMapping("/api/notifications/email")
    public ResponseEntity<Void> sendEmailLegacy(@RequestBody NotificationDto dto) {
        notificationService.sendEmail(dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/v1/notifications/my")
    public ResponseEntity<ApiResponse<List<NotificationLog>>> getMyNotifications(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<NotificationLog> logs = notificationService.getMyNotifications(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved successfully", logs));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @GetMapping("/api/v1/admin/notifications")
    public ResponseEntity<ApiResponse<List<NotificationLog>>> getAllNotifications() {
        List<NotificationLog> logs = notificationService.getAllNotifications();
        return ResponseEntity.ok(ApiResponse.success("All notification audit logs retrieved", logs));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @GetMapping("/api/v1/admin/notifications/providers")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProviderStatus() {
        Map<String, Object> status = notificationService.getProviderStatus();
        return ResponseEntity.ok(ApiResponse.success("Notification provider configurations", status));
    }
}
