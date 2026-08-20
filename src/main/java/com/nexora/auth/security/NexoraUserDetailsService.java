package com.nexora.auth.security;

import com.nexora.user.entity.User;
import com.nexora.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * WHAT IS UserDetailsService?
 * This is the one method Spring Security calls whenever it needs to
 * look up "who is this user, by their identifier" — during login, and
 * potentially when re-validating a session. We implement it by
 * fetching from MySQL via UserRepository and wrapping the result in
 * our UserPrincipal adapter (see UserPrincipal.java).
 *
 * Note: for Nexora, "username" in Spring Security terms IS the email
 * address — we never introduced a separate username field.
 */
@Service
@RequiredArgsConstructor
public class NexoraUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user found with email: " + email));

        return new UserPrincipal(user);
    }
}
