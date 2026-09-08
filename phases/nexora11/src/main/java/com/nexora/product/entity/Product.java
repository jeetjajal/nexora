package com.nexora.product.entity;

import com.nexora.category.entity.Category;
import com.nexora.store.entity.Store;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * RELATIONSHIPS ON Product:
 *
 * 1) Product (Many) -> Store (One)             [@ManyToOne]
 *    Each product belongs to exactly one store; a store has many products.
 *
 * 2) Product (Many) <-> Category (Many)         [@ManyToMany]
 *    Via the product_categories join table, same idea as Store's
 *    categories above.
 *
 * 3) Product (One) <-> Inventory (One)          [@OneToOne, mapped on
 *    the Inventory side — see Inventory.java]. We deliberately do NOT
 *    put an @OneToOne field here on Product pointing to Inventory,
 *    to avoid two entities both "owning" the same relationship and to
 *    keep stock-related concerns isolated in their own module —
 *    inventory has its own repository/service and (from Phase 6
 *    onward) its own concurrency-control logic.
 *
 * NOTE ON price: we use BigDecimal, never double/float, for money.
 * Floating point types (double/float) cannot represent decimal
 * fractions like 0.10 exactly in binary, which causes rounding errors
 * that compound over many transactions. BigDecimal stores exact
 * decimal values and is the standard choice for currency in Java.
 */
@Entity
@Table(
        name = "products",
        indexes = {
                @Index(name = "idx_products_store_id", columnList = "store_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Discount stored as a percentage (e.g. 15.00 = 15% off),
     * applied by the service layer when computing the final price —
     * never trust a discount value coming from the frontend at
     * checkout time (Phase 7/8 enforce this in the Cart/Order flow).
     */
    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean available = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "product_categories",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @Builder.Default
    private Set<Category> categories = new HashSet<>();

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
