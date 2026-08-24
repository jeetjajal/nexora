package com.nexora.inventory.entity;

import com.nexora.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * RELATIONSHIP: Inventory (One) <-> Product (One)  [@OneToOne]
 *
 * Every product has exactly one inventory record tracking its stock.
 * We put the foreign key (product_id) here on Inventory and mark it
 * `unique = true`, which is what makes this a true one-to-one instead
 * of a one-to-many: the database itself will reject a second Inventory
 * row for the same product_id.
 *
 * WHY A SEPARATE TABLE INSTEAD OF A "stock" COLUMN ON Product?
 * Stock changes constantly and is touched by high-concurrency,
 * high-frequency operations (every order, every cancellation).
 * Keeping it in its own table means:
 *   - Inventory updates don't lock/rewrite the whole Product row
 *     (which rarely changes: name, description, price).
 *   - From Phase 6 onward, this table gets its own transaction and
 *     locking strategy (optimistic locking via @Version, Phase 21)
 *     without touching Product at all.
 */
@Entity
@Table(name = "inventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 0;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.updatedAt = LocalDateTime.now();
    }
}
