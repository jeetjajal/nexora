package com.nexora.category.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "subcategories",
        indexes = {
                @Index(name = "idx_subcat_category", columnList = "category_id"),
                @Index(name = "idx_subcat_slug", columnList = "slug")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subcategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 120)
    private String slug;

    @Column(length = 500)
    private String description;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
