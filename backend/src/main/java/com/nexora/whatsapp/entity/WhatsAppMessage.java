package com.nexora.whatsapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "whatsapp_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsAppMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private WhatsAppConversation conversation;

    @Column(nullable = false, length = 20)
    private String direction; // INBOUND, OUTBOUND

    @Column(name = "message_id", length = 100)
    private String messageId;

    @Column(name = "message_type", nullable = false, length = 30)
    @Builder.Default
    private String messageType = "TEXT"; // TEXT, INTERACTIVE, ORDER_LINK

    @Column(name = "message_body", columnDefinition = "TEXT")
    private String messageBody;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
