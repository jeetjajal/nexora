package com.nexora.store.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "temperature_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemperatureLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "storage_unit", nullable = false, length = 60)
    private String storageUnit; // CHILLER_01, FREEZER_01, TRANSIT_COOLBOX_12

    @Column(name = "temperature_celsius", nullable = false)
    private Double temperatureCelsius;

    @Column(name = "humidity_percentage")
    private Double humidityPercentage;

    @Column(name = "min_safe_temp", nullable = false)
    @Builder.Default
    private Double minSafeTemp = 2.0;

    @Column(name = "max_safe_temp", nullable = false)
    @Builder.Default
    private Double maxSafeTemp = 8.0;

    @Column(name = "breach_detected", nullable = false)
    @Builder.Default
    private Boolean breachDetected = false;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.recordedAt == null) {
            this.recordedAt = LocalDateTime.now();
        }
    }
}
