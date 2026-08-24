package com.nexora.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * WHAT IS AN "ENTITY"?
 * An @Entity class is a plain Java class that Spring Data JPA maps directly
 * to a database table. Each field becomes a column. Hibernate (the JPA
 * implementation Spring Boot uses) reads these annotations and generates
 * the SQL for you (CREATE TABLE, INSERT, UPDATE, SELECT...) so you rarely
 * write raw SQL by hand.
 *
 * This class maps to the "users" table described in the Nexora database design.
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /**
     * IMPORTANT: this column stores a BCrypt HASH of the password,
     * never the plain text password. See UserService for how hashing
     * happens before saving.
     */
    @Column(nullable = false)
    private String password;

    @Column(length = 20)
    private String phone;

    /**
     * Simple account status for now (e.g. ACTIVE, SUSPENDED).
     * Kept as a plain String in Phase 1 to keep things simple;
     * can be upgraded to an enum-backed column later if needed.
     */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    /**
     * Role is kept simple in Phase 1 (single role per user, as a string).
     * A dedicated roles / user_roles table (many-to-many) is introduced
     * in Phase 4 when we build full Spring Security + JWT authorization.
     */
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String role = "CUSTOMER";

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
