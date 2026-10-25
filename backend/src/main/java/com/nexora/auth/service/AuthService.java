package com.nexora.auth.service;

import com.nexora.auth.dto.AuthResponse;
import com.nexora.auth.dto.LoginRequest;
import com.nexora.auth.exception.AccountNotActiveException;
import com.nexora.auth.exception.InvalidCredentialsException;
import com.nexora.auth.security.JwtService;
import com.nexora.auth.security.UserPrincipal;
import com.nexora.user.entity.User;
import com.nexora.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * WHERE LOGIN LOGIC LIVES.
 *
 * Registration logic stays in UserService (Phase 1/2) — AuthService
 * does NOT duplicate it. AuthService is only responsible for turning
 * a LoginRequest into a signed JWT, per this phase's architecture
 * diagram: AuthController -> AuthService -> AuthenticationManager
 * (which internally uses PasswordEncoder + UserRepository via
 * NexoraUserDetailsService) -> JwtService -> JWT token.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${nexora.jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    public AuthResponse login(LoginRequest request) {

        Authentication authentication;
        try {
            // This one call is where the real work happens:
            // Spring Security's AuthenticationManager delegates to our
            // DaoAuthenticationProvider (configured in SecurityConfig),
            // which uses NexoraUserDetailsService to load the user by
            // email, then PasswordEncoder.matches() to compare the
            // submitted password against the stored BCrypt hash.
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (DisabledException | LockedException ex) {
            // Thrown by Spring Security itself when UserDetails.isEnabled()
            // or isAccountNonLocked() returns false — i.e. our
            // UserPrincipal already told it this account isn't ACTIVE.
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(InvalidCredentialsException::new);
            throw new AccountNotActiveException(user.getStatus());
        } catch (BadCredentialsException ex) {
            // Wrong password OR unknown email — same exception either
            // way, deliberately (see InvalidCredentialsException's javadoc).
            throw new InvalidCredentialsException();
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = principal.getUser();

        String accessToken = jwtService.generateToken(principal);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresInMs(accessTokenExpirationMs)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet()))
                .build();
    }
}
