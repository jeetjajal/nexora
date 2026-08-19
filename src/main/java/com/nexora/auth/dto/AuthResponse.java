package com.nexora.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

/**
 * Returned by POST /api/v1/auth/login (and could optionally be
 * returned by register too, in a later phase, for "log in
 * immediately after signing up" UX — Phase 3 keeps register and login
 * as separate, explicit steps per the given scope).
 *
 * The client stores `accessToken` and sends it back as:
 *   Authorization: Bearer <accessToken>
 * on every subsequent request to a protected endpoint.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String accessToken;
    private String tokenType; // always "Bearer"
    private Long expiresInMs;

    private Long userId;
    private String name;
    private String email;
    private Set<String> roles;
}
