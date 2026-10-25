package com.nexora.cart.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.nexora.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * RELATIONSHIP: CartItem (Many) -> Cart (One), CartItem (Many) -> Product (One)
 *
 * DELIBERATE DESIGN CHOICE: no `price` column here. Unlike an
 * OrderItem (Phase 8), which snapshots the price at the moment of
 * purchase (so a later price change never rewrites history for a
 * completed order), a CartItem always reflects the product's CURRENT
 * live price — CartService reads `product.getPrice()` fresh every
 * time the cart is viewed. This directly satisfies "the backend must
 * retrieve current product prices from MySQL and never trust the
 * frontend price": there's no stored price for the frontend to have
 * ever supplied in the first place, and no stale cached price that
 * could drift from what checkout would actually charge.
 */
@Entity
@Table(
        name = "cart_items",
        uniqueConstraints = {
                // A cart can only have ONE row per product — adding the
                // same product again increases its quantity instead of
                // creating a duplicate row (see CartService.addItem).
                @UniqueConstraint(name = "uk_cart_items_cart_product", columnNames = {"cart_id", "product_id"})
        },
        indexes = {
                @Index(name = "idx_cart_items_cart_id", columnList = "cart_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    @JsonBackReference
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

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
