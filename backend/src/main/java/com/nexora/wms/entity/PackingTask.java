package com.nexora.wms.entity;

import com.nexora.order.entity.Order;
import com.nexora.store.entity.Store;
import com.nexora.wms.enums.PackingTaskStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "packing_tasks",
        indexes = {
                @Index(name = "idx_pkt_order", columnList = "order_id"),
                @Index(name = "idx_pkt_store", columnList = "store_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackingTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "packer_id")
    private Long packerId;

    @Column(name = "bag_code", nullable = false, length = 64)
    private String bagCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PackingTaskStatus status = PackingTaskStatus.PENDING;

    @Column(name = "qc_passed", nullable = false)
    @Builder.Default
    private Boolean qcPassed = true;

    @Column(name = "missing_items_count", nullable = false)
    @Builder.Default
    private Integer missingItemsCount = 0;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

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
