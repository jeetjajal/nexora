package com.nexora.wms.entity;

import com.nexora.order.entity.Order;
import com.nexora.store.entity.Store;
import com.nexora.wms.enums.PickPathAlgorithm;
import com.nexora.wms.enums.PickTaskStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "pick_tasks",
        indexes = {
                @Index(name = "idx_pt_order", columnList = "order_id"),
                @Index(name = "idx_pt_store", columnList = "store_id"),
                @Index(name = "idx_pt_picker", columnList = "picker_id"),
                @Index(name = "idx_pt_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PickTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_code", nullable = false, unique = true, length = 64)
    private String taskCode;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "picker_id")
    private PickerProfile picker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PickTaskStatus status = PickTaskStatus.PENDING;

    @Column(name = "total_items_count", nullable = false)
    @Builder.Default
    private Integer totalItemsCount = 0;

    @Column(name = "picked_items_count", nullable = false)
    @Builder.Default
    private Integer pickedItemsCount = 0;

    @Column(name = "estimated_distance_meters", nullable = false, precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal estimatedDistanceMeters = BigDecimal.ZERO;

    @Column(name = "estimated_time_seconds", nullable = false)
    @Builder.Default
    private Integer estimatedTimeSeconds = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "selected_algorithm", nullable = false, length = 30)
    @Builder.Default
    private PickPathAlgorithm selectedAlgorithm = PickPathAlgorithm.S_SHAPE;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "exception_reason", length = 255)
    private String exceptionReason;

    @OneToMany(mappedBy = "pickTask", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PickTaskItem> items = new ArrayList<>();

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
