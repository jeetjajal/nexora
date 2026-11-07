package com.nexora.wms.entity;

import com.nexora.store.entity.Store;
import com.nexora.wms.enums.RackSide;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "store_racks",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_aisle_rack", columnNames = {"aisle_id", "rack_code"})
        },
        indexes = {
                @Index(name = "idx_sr_aisle", columnList = "aisle_id"),
                @Index(name = "idx_sr_store", columnList = "store_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreRack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aisle_id", nullable = false)
    private StoreAisle aisle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "rack_code", nullable = false, length = 20)
    private String rackCode;

    @Column(name = "rack_number", nullable = false)
    private Integer rackNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private RackSide side = RackSide.LEFT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
