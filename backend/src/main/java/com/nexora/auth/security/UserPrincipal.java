package com.nexora.auth.security;

import com.nexora.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * WHAT THIS CLASS IS:
 * Spring Security doesn't know what our custom "User" entity looks
 * like — it only speaks its own "UserDetails" interface (which defines
 * getUsername, getPassword, getAuthorities, isEnabled, etc.).
 *
 * UserPrincipal is an "adapter" that wraps our JPA User entity and
 * exposes it as a Spring Security UserDetails object. This lets us:
 *   1. Keep our domain entity clean (User doesn't have to implement
 *      Spring Security interfaces directly).
 *   2. Give Spring Security everything it needs to check passwords,
 *      roles, and account status during authentication.
 *   3. Expose the underlying User back to our controllers via
 *      @AuthenticationPrincipal UserPrincipal principal, so we always
 *      know WHO is calling an endpoint without an extra DB lookup.
 *
 * PHASE 4 ADDITION: getAuthorities() now maps our database roles
 * (ROLE_CUSTOMER, ROLE_RIDER, ROLE_STORE_OWNER, ROLE_ADMIN) into Spring
 * Security's SimpleGrantedAuthority objects with the "ROLE_" prefix.
 * This is what lets us use annotations like
 * @PreAuthorize("hasRole('ADMIN')") and have it just work.
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final User user;

    public UserPrincipal(User user) {
        this.user = user;
    }

    public Long getId() {
        return user != null ? user.getId() : null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (user == null || user.getRoles() == null) {
            return Collections.emptySet();
        }

        Set<String> roleNames = user.getRoles().stream()
                .map(role -> "ROLE_" + role.getName().name())
                .collect(Collectors.toSet());

        return roleNames.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return user != null ? user.getPassword() : null;
    }

    @Override
    public String getUsername() {
        return user != null ? user.getEmail() : null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user != null && !"SUSPENDED".equalsIgnoreCase(user.getStatus());
    }
}
