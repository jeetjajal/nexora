package com.nexora.address.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.nexora.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * RELATIONSHIP: Address (Many) -> User (One)
 * One user can have many saved addresses (home, work, etc.), but each
 * address belongs to exactly one user. That's a classic @ManyToOne
 * from Address's side, mirrored by @OneToMany on User's side.
 *
 * FetchType.LAZY (explained below in User.addresses) is used here too —
 * loading an Address should NOT automatically pull in the full User
 * object unless we explicitly ask for it.
 */
@Entity
@Table(
        name = "addresses",
        indexes = {
                @Index(name = "idx_addresses_user_id", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;

    @Column(length = 30)
    private String label; // e.g. "Home", "Work"

    @Column(name = "address_line1", nullable = false, length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(nullable = false, length = 10)
    private String pincode;

    @Column(nullable = false, length = 100)
    @Builder.Default
    private String country = "India";

    private Double latitude;
    private Double longitude;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;

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
