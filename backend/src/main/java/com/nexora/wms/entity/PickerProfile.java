package com.nexora.wms.entity;

import com.nexora.store.entity.Store;
import com.nexora.user.entity.User;
import com.nexora.wms.enums.PickerStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "picker_profiles",
        indexes = {
                @Index(name = "idx_pp_user", columnList = "user_id"),
                @Index(name = "idx_pp_store", columnList = "store_id"),
                @Index(name = "idx_pp_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PickerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "employee_code", nullable = false, unique = true, length = 30)
    private String employeeCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PickerStatus status = PickerStatus.AVAILABLE;

    @Column(name = "current_task_id")
    private Long currentTaskId;

    @Column(name = "completed_tasks_count", nullable = false)
    @Builder.Default
    private Integer completedTasksCount = 0;

    @Column(name = "average_pick_seconds_per_item", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal averagePickSecondsPerItem = new BigDecimal("8.00");

    @Column(name = "shift_started_at")
    private LocalDateTime shiftStartedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
