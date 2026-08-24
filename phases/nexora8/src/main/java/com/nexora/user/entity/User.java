package com.nexora.user.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.nexora.address.entity.Address;
import com.nexora.role.entity.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * WHAT IS AN "ENTITY"?
 * An @Entity class is a plain Java class that Spring Data JPA maps directly
 * to a database table. Each field becomes a column. Hibernate (the JPA
 * implementation Spring Boot uses) reads these annotations and generates
 * the SQL for you (CREATE TABLE, INSERT, UPDATE, SELECT...) so you rarely
 * write raw SQL by hand.
 *
 * This class maps to the "users" table described in the Nexora database design.
 *
 * PHASE 2 UPDATE:
 * Phase 1 had a simple `role` string column directly on User. From
 * Phase 2 onward we replace that with a proper many-to-many
 * relationship to a real Role entity (see role/entity/Role.java for
 * why). All Phase 1 behavior (register, get-by-id) still works the
 * same from the outside — only how roles are stored internally changed.
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
     * RELATIONSHIP: User (Many) <-> Role (Many)   [@ManyToMany]
     *
     * This is the OWNING side of the relationship (it declares the
     * @JoinTable), meaning Hibernate creates and manages a
     * "user_roles" join table with columns user_id and role_id.
     * We never write SQL for that join table ourselves — adding a
     * Role to this Set and saving the User is enough.
     *
     * FetchType.EAGER here (unlike most other relationships in this
     * project) is a deliberate exception: roles are small (max 4 rows
     * total), rarely change, and are needed on almost every
     * authenticated request in Phase 3 for authorization checks — so
     * the small cost of always loading them is worth avoiding an
     * extra query almost every time we need to check "is this user an
     * ADMIN?".
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    /**
     * RELATIONSHIP: User (One) <-> Address (Many)   [@OneToMany]
     *
     * `mappedBy = "user"` means Address is the OWNING side (it holds
     * the actual user_id foreign key column — see Address.java).
     * This side is just the "inverse" view: "give me all addresses
     * where address.user == this user". Declaring it this way (instead
     * of duplicating the foreign key here) avoids two tables both
     * trying to manage the same foreign key column.
     *
     * FetchType.LAZY: a user's addresses are only queried from the
     * database when user.getAddresses() is actually called — e.g. on
     * the checkout page — not every single time we load a User object
     * (which would be wasteful for, say, just checking someone's name).
     *
     * cascade = CascadeType.ALL: if a User is deleted, their addresses
     * are deleted with them (an address with no owner is meaningless).
     * orphanRemoval = true: if an Address is removed from this set and
     * the User is saved, that Address row is deleted from the database
     * too, not just unlinked.
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    @Builder.Default
    private Set<Address> addresses = new HashSet<>();

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
