package com.nexora.wms.entity;

import com.nexora.wms.enums.PickingEventType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "picking_events",
        indexes = {
                @Index(name = "idx_pe_task", columnList = "pick_task_id"),
                @Index(name = "idx_pe_operator", columnList = "operator_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PickingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pick_task_id", nullable = false)
    private PickTask pickTask;

    @Column(name = "pick_task_item_id")
    private Long pickTaskItemId;

    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    @Column(name = "scanned_raw_value", nullable = false, length = 100)
    private String scannedRawValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private PickingEventType eventType;

    @Column(name = "is_success", nullable = false)
    @Builder.Default
    private Boolean isSuccess = true;

    @Column(name = "error_message", length = 255)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
