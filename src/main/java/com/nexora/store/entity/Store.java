package com.nexora.store.entity;

import com.nexora.category.entity.Category;
import com.nexora.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

/**
 * RELATIONSHIPS ON Store:
 *
 * 1) Store (Many) -> User/owner (One)         [@ManyToOne]
 *    One STORE_OWNER user can own multiple stores; each store has
 *    exactly one owner. We store owner_id as a foreign key here.
 *
 * 2) Store (Many) <-> Category (Many)          [@ManyToMany]
 *    A store can belong to several categories ("Pizza" AND "Fast Food"),
 *    and a category can obviously apply to many stores. This needs a
 *    join table — store_categories — which JPA manages for us via
 *    @JoinTable; we never touch that table directly in Java.
 */
@Entity
@Table(
        name = "stores",
        indexes = {
                @Index(name = "idx_stores_owner_id", columnList = "owner_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StoreStatus status = StoreStatus.OPEN;

    @Column(name = "opening_time")
    private LocalTime openingTime;

    @Column(name = "closing_time")
    private LocalTime closingTime;

    /**
     * @ManyToMany owning side. The @JoinTable annotation tells
     * Hibernate to create and manage a "store_categories" join table
     * with columns store_id and category_id — we never write SQL
     * for this table ourselves.
     *
     * FetchType.LAZY: categories are only loaded from the database
     * when store.getCategories() is actually called, not automatically
     * every time a Store is fetched. This avoids pulling in extra data
     * (and extra SQL joins) we might not need for a given request —
     * important for keeping list endpoints (e.g. "browse stores") fast.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "store_categories",
            joinColumns = @JoinColumn(name = "store_id"),
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
