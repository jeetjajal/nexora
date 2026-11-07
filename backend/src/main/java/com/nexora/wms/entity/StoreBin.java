package com.nexora.wms.entity;

import com.nexora.store.entity.Store;
import com.nexora.wms.enums.BinStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "store_bins",
        indexes = {
                @Index(name = "idx_sb_shelf", columnList = "shelf_id"),
                @Index(name = "idx_sb_store", columnList = "store_id"),
                @Index(name = "idx_sb_traversal", columnList = "store_id, traversal_sequence")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreBin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shelf_id", nullable = false)
    private StoreShelf shelf;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "bin_code", nullable = false, length = 20)
    private String binCode;

    @Column(name = "full_location_code", nullable = false, unique = true, length = 80)
    private String fullLocationCode;

    @Column(name = "traversal_sequence", nullable = false)
    @Builder.Default
    private Integer traversalSequence = 0; // Pre-computed S-Shape sorting index

    @Column(name = "x_coord", nullable = false, precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal xCoord = BigDecimal.ZERO; // 2D warehouse X coordinate (meters)

    @Column(name = "y_coord", nullable = false, precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal yCoord = BigDecimal.ZERO; // 2D warehouse Y coordinate (meters)

    @Column(name = "capacity_units", nullable = false)
    @Builder.Default
    private Integer capacityUnits = 100;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BinStatus status = BinStatus.ACTIVE;

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
