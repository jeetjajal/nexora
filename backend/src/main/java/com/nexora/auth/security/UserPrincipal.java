package com.nexora.auth.security;

import com.nexora.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * WHAT IS UserDetails?
 * Spring Security doesn't know anything about our `User` entity — it
 * works against its own interface, `UserDetails`. This class is a thin
 * ADAPTER: it wraps our real `User` entity and answers the specific
 * questions Spring Security asks (what's the username? the password?
 * what authorities/roles does this user have? is the account enabled?).
 *
 * We keep the original `User` entity accessible via getUser() so the
 * rest of our code (controllers, JWT generation) can still get the
 * real id, name, email, etc. without re-querying the database.
 *
 * WHY "authorities" AND NOT JUST "roles"?
 * Spring Security's authorization model is built around
 * GrantedAuthority strings. By convention, role-based checks
 * (hasRole("ADMIN")) look for an authority literally named "ROLE_ADMIN"
 * — so we prefix each Role with "ROLE_" here. This lets us later write
 * @PreAuthorize("hasRole('ADMIN')") and have it just work.
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final User user;

    public UserPrincipal(User user) {
        this.user = user;
    }

    public Long getId() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<String> roleNames = user.getRoles().stream()
                .map(role -> "ROLE_" + role.getName().name())
                .collect(Collectors.toSet());

        return roleNames.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        // We authenticate by email, not a separate "username" field.
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Nexora doesn't implement account expiry in Phase 3
    }

    @Override
    public boolean isAccountNonLocked() {
        // A SUSPENDED account is treated as locked out — they can never
        // successfully authenticate while their status is SUSPENDED.
        return !"SUSPENDED".equalsIgnoreCase(user.getStatus());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // no password-expiry policy yet
    }

    @Override
    public boolean isEnabled() {
        return "ACTIVE".equalsIgnoreCase(user.getStatus());
    }
}
