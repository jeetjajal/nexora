package com.nexora.wms.entity;

import com.nexora.store.entity.Store;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "store_shelves",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_rack_shelf", columnNames = {"rack_id", "shelf_code"})
        },
        indexes = {
                @Index(name = "idx_ss_rack", columnList = "rack_id"),
                @Index(name = "idx_ss_store", columnList = "store_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreShelf {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rack_id", nullable = false)
    private StoreRack rack;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "shelf_code", nullable = false, length = 20)
    private String shelfCode;

    @Column(name = "shelf_level", nullable = false)
    @Builder.Default
    private Integer shelfLevel = 1; // 1 (bottom), 2 (middle), 3 (eye-level), 4 (top)

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
