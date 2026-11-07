package com.nexora.wms.entity;

import com.nexora.order.entity.OrderItem;
import com.nexora.product.entity.Product;
import com.nexora.store.entity.InventoryBatch;
import com.nexora.wms.enums.PickItemStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "pick_task_items",
        indexes = {
                @Index(name = "idx_pti_task", columnList = "pick_task_id"),
                @Index(name = "idx_pti_order_item", columnList = "order_item_id"),
                @Index(name = "idx_pti_prod", columnList = "product_id"),
                @Index(name = "idx_pti_bin", columnList = "bin_id"),
                @Index(name = "idx_pti_batch", columnList = "batch_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PickTaskItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pick_task_id", nullable = false)
    private PickTask pickTask;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bin_id")
    private StoreBin bin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private InventoryBatch batch; // FEFO reserved batch reference

    @Column(name = "sequence_index", nullable = false)
    @Builder.Default
    private Integer sequenceIndex = 1; // S-Shape ordered visit sequence

    @Column(name = "required_quantity", nullable = false)
    @Builder.Default
    private Integer requiredQuantity = 1;

    @Column(name = "picked_quantity", nullable = false)
    @Builder.Default
    private Integer pickedQuantity = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PickItemStatus status = PickItemStatus.PENDING;

    @Column(name = "scanned_barcode", length = 64)
    private String scannedBarcode;

    @Column(name = "picked_at")
    private LocalDateTime pickedAt;

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
