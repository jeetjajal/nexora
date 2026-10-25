package com.nexora.category.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * A single, simple Category table (e.g. "Pizza", "Grocery", "Desserts")
 * that BOTH Store and Product can attach to. Two separate join tables
 * connect it — store_categories and product_categories — because a
 * store's categories (what kind of store it is) and a product's
 * categories (what kind of item it is) are conceptually different
 * many-to-many relationships, even though they share the same
 * underlying category vocabulary.
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 255)
    private String imageUrl;
}
