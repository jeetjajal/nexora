package com.nexora.cart.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.nexora.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * RELATIONSHIP: Cart (One) <-> User (One)  [@OneToOne]
 *
 * Every user gets at most one cart, created lazily the first time they
 * touch the cart API (see CartService.getOrCreateCartForUser) rather
 * than at registration time — most users may never add anything to a
 * cart, so there's no reason to create an empty row for every signup.
 *
 * We deliberately did NOT add a `cart` field back on the User entity
 * itself (unlike, say, User.addresses in Phase 2) — Cart can always be
 * looked up via CartRepository.findByUserId(...), and avoiding a new
 * field on User keeps this phase's footprint isolated to the new
 * `cart` package, touching nothing in the `user` package at all.
 */
@Entity
@Table(name = "carts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * cascade = ALL + orphanRemoval = true: removing a CartItem from
     * this set (and saving the Cart) deletes that row from the
     * database, not just unlinks it — exactly the same pattern Phase 2
     * used for User.addresses, applied here for the same reason (a
     * CartItem with no cart is meaningless).
     */
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    @Builder.Default
    private Set<CartItem> items = new HashSet<>();

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
