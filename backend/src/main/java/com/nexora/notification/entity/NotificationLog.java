package com.nexora.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "notification_logs", indexes = {
        @Index(name = "idx_notif_dedup", columnList = "deduplicationKey", unique = true),
        @Index(name = "idx_notif_order", columnList = "orderId"),
        @Index(name = "idx_notif_recipient", columnList = "recipient")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    @Column(nullable = false)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 48)
    private NotificationEventType eventType;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String sanitizedBody;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationDeliveryStatus deliveryStatus;

    @Column(length = 512)
    private String providerResponse;

    @Column(length = 128, unique = true)
    private String deduplicationKey;

    @Builder.Default
    private int retryCount = 0;

    @Builder.Default
    private Instant createdAt = Instant.now();

    private Instant deliveredAt;
}
