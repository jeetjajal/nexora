package com.nexora.pricing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "surge_pricing_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurgePricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hub_id", nullable = false)
    private Long hubId;

    @Column(name = "hub_name", nullable = false, length = 100)
    private String hubName;

    @Column(name = "zone_name", nullable = false, length = 100)
    private String zoneName;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = false;

    /** Delivery fee multiplier (1.0 = normal, max cap 2.5) */
    @Column(nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal multiplier = BigDecimal.ONE;

    /** Extra flat surge fee in ₹ (e.g. ₹15 during heavy rainfall) */
    @Column(name = "extra_flat_fee", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal extraFlatFee = BigDecimal.ZERO;

    @Column(length = 200)
    private String reason;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
