package com.nexora.wms.entity;

import com.nexora.store.entity.Store;
import com.nexora.wms.enums.ZoneTemperatureType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "store_zones",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_store_zone", columnNames = {"store_id", "zone_code"})
        },
        indexes = {
                @Index(name = "idx_sz_store", columnList = "store_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "zone_code", nullable = false, length = 30)
    private String zoneCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "temperature_type", nullable = false, length = 30)
    @Builder.Default
    private ZoneTemperatureType temperatureType = ZoneTemperatureType.AMBIENT;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

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
