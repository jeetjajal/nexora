package com.nexora.wms.entity;

import com.nexora.store.entity.Store;
import com.nexora.wms.enums.AisleDirection;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "store_aisles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_store_aisle", columnNames = {"store_id", "aisle_code"})
        },
        indexes = {
                @Index(name = "idx_sa_zone", columnList = "zone_id"),
                @Index(name = "idx_sa_store", columnList = "store_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreAisle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private StoreZone zone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "aisle_code", nullable = false, length = 20)
    private String aisleCode;

    @Column(name = "aisle_number", nullable = false)
    private Integer aisleNumber; // 1, 2, 3... used for S-Shape sequence ordering

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AisleDirection direction = AisleDirection.BIDIRECTIONAL;

    @Column(name = "is_restricted", nullable = false)
    @Builder.Default
    private Boolean isRestricted = false;

    @Column(name = "width_meters", nullable = false, precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal widthMeters = new BigDecimal("1.80");

    @Column(name = "length_meters", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal lengthMeters = new BigDecimal("12.00");

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
