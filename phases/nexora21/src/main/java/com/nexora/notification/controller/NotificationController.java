package com.nexora.notification.controller;

import com.nexora.notification.dto.NotificationDto;
import com.nexora.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    @Autowired
    private NotificationService notificationService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/email")
    public ResponseEntity<Void> sendEmail(@RequestBody NotificationDto dto) {
        notificationService.sendEmail(dto);
        return ResponseEntity.ok().build();
    }
}
