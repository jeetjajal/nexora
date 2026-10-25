package com.nexora.order.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.nexora.address.entity.Address;
import com.nexora.store.entity.Store;
import com.nexora.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * RELATIONSHIPS:
 *   Order (Many) -> User/customer (One)    [@ManyToOne]
 *   Order (Many) -> Store (One)            [@ManyToOne]
 *   Order (Many) -> Address (One)          [@ManyToOne] - delivery destination
 *   Order (One) <-> OrderItem (Many)       [@OneToMany]
 *
 * WHY ONE STORE PER ORDER?
 * A customer's Cart (Phase 7) can hold products from several
 * different stores at once, but a real order/fulfillment/delivery
 * happens per-store - one restaurant prepares and hands off one
 * package; it can't jointly fulfill an order that's half groceries
 * from one store and half pizza from another. So checkout (see
 * OrderService.checkout) splits a multi-store cart into one Order PER
 * STORE, each with only that store's items. This also matches the
 * `orders` table design from the very first project brief, which
 * already specified a single store_id column per order.
 *
 * WHY totalAmount, subtotal, deliveryFee ARE ALL STORED (not derived
 * on the fly like Cart's subtotal was):
 * A Cart's totals must always reflect LIVE product prices (Phase 7) -
 * nothing has been purchased yet. An Order is the opposite: it's a
 * historical record of what was actually agreed to at checkout time.
 * If a store later changes a product's price, every past order must
 * still show what the customer actually paid, unchanged forever. So
 * these totals are computed once, at order-creation time, and stored
 * - never recalculated from current Product data afterward.
 */
@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_orders_user_id", columnList = "user_id"),
                @Index(name = "idx_orders_store_id", columnList = "store_id"),
                @Index(name = "idx_orders_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_address_id", nullable = false)
    private Address deliveryAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private OrderStatus status = OrderStatus.CREATED;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "delivery_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryFee;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    @Builder.Default
    private Set<OrderItem> items = new HashSet<>();

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
