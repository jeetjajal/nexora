package com.nexora.role.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * WHY A SEPARATE Role ENTITY INSTEAD OF A STRING COLUMN?
 * In Phase 1 we kept a simple `role` string on User to get moving fast.
 * From Phase 2 onward we model roles properly as their own table:
 *
 *   roles            -> this entity (id, name)
 *   user_roles       -> join table connecting users <-> roles
 *
 * Why bother, instead of a column like user.role = "CUSTOMER"?
 *   1. A user can hold MORE THAN ONE role in real life — e.g. a person
 *      might be both a CUSTOMER and a DELIVERY_PARTNER on the same
 *      account. A single string column can't express that; a
 *      many-to-many relationship can.
 *   2. Role-based authorization (Phase 3, Spring Security) reads
 *      GrantedAuthority values from a proper roles collection —
 *      this is the standard, idiomatic Spring Security shape.
 *   3. It keeps role data normalized: if we ever needed to attach
 *      metadata to a role (e.g. permissions), there's a real table
 *      to attach it to.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 30)
    private RoleName name;
}
