package com.nexora.order.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.nexora.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Snapshots product price, name, quantity at order time, and tracks picking state for store operations.
 */
@Entity
@Table(
        name = "order_items",
        indexes = {
                @Index(name = "idx_order_items_order_id", columnList = "order_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonBackReference
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Snapshotted at order time — survives the product being renamed later. */
    @Column(name = "product_name_snapshot", nullable = false, length = 150)
    private String productNameSnapshot;

    /** Snapshotted at order time (product.price - product.discount at that moment). */
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    /** unitPrice * quantity, also snapshotted (never recomputed from live data). */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    /** Store operations & picker tracking */
    @Column(name = "is_picked")
    @Builder.Default
    private Boolean isPicked = false;

    @Column(name = "picked_at")
    private LocalDateTime pickedAt;

    @Column(name = "picked_by", length = 100)
    private String pickedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.isPicked == null) {
            this.isPicked = false;
        }
    }
}
