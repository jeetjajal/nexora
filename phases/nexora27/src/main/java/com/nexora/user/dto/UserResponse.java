package com.nexora.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * This is what we SEND BACK to the client after registration/lookup.
 * Notice: NO password field here. That's the whole point of a
 * response DTO — it controls exactly what leaves our system.
 *
 * PHASE 2 UPDATE: `role` (single string) is now `roles` (a set of
 * strings) since User now has a many-to-many relationship with Role.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private String status;
    private Set<String> roles;
    private LocalDateTime createdAt;
}
