package com.nexora.notification.service;

import com.nexora.notification.dto.NotificationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);

    public void sendEmail(NotificationDto dto) {
        LOG.info("[Notification] Sending email to={}, subject={}, body={}", dto.getTo(), dto.getSubject(), dto.getBody());
    }
}
