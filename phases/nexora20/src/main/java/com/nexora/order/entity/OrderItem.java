package com.nexora.order.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.nexora.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * UNLIKE CartItem (Phase 7), which stores NO price and always reads
 * the product's current price live, OrderItem SNAPSHOTS both the
 * price and the product name at the moment of purchase into
 * `unitPrice` and `productNameSnapshot`. This is deliberate and
 * important: once an order exists, it's a permanent historical
 * record. If the store later changes the product's price or renames
 * it, every past order must still show exactly what the customer
 * actually paid for exactly what they actually ordered — not
 * whatever the product happens to look like today.
 *
 * `product` is still kept as a real foreign key (not just a copied
 * name) so the product can still be looked up/linked to for anything
 * that needs it (e.g. "buy this again"). To keep that link safe,
 * ProductService.deleteProduct (Phase 4) now checks
 * OrderItemRepository.existsByProductId before allowing a hard delete
 * — see ProductService for that guard, added in this phase.
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

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
