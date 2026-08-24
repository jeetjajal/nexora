package com.nexora.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * THE CENTRAL SECURITY CONFIGURATION.
 *
 * This is where we declare, in one place:
 *   - Which endpoints are PUBLIC (no token needed) vs PROTECTED.
 *   - That Nexora uses STATELESS sessions (no server-side session
 *     storage — every request must carry its own JWT).
 *   - Where our custom JwtAuthenticationFilter plugs into Spring
 *     Security's filter chain.
 *   - How passwords get hashed (BCryptPasswordEncoder).
 *   - How Spring Security should look up users during login
 *     (DaoAuthenticationProvider + our NexoraUserDetailsService).
 *
 * @EnableWebSecurity turns on Spring Security's web support and lets
 * us override its default behavior with the beans below.
 *
 * PHASE 4 ADDITION: @EnableMethodSecurity turns on @PreAuthorize
 * support, so individual controller methods (StoreController,
 * CategoryController, ProductController, InventoryController) can
 * declare their own role requirements, e.g.
 * @PreAuthorize("hasAnyRole('STORE_OWNER','ADMIN')"), instead of every
 * rule having to live in this one class's URL-pattern list. This is
 * purely additive — nothing about the Phase 3 login/JWT flow changed.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    /**
     * BCrypt: a one-way, intentionally-slow password hashing algorithm.
     * "Intentionally slow" is a feature, not a bug — it makes
     * brute-force password-guessing attacks impractically expensive,
     * while a single real login (one hash check) still feels instant
     * to a real user.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Tells Spring Security HOW to authenticate a login attempt:
     * look the user up via our UserDetailsService, then compare the
     * submitted password against the stored hash using our
     * PasswordEncoder. This is standard "DAO-backed" authentication —
     * DAO here just means "backed by a database lookup."
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * AuthenticationManager is what AuthService actually calls to
     * attempt a login (see AuthService.login()). Spring Boot's
     * AuthenticationConfiguration already assembles one for us out of
     * the AuthenticationProvider bean above — we just expose it.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * THE MAIN SECURITY RULES.
     *
     * - CSRF protection is disabled: CSRF defends against a browser
     *   automatically attaching cookies/session state to a forged
     *   request. Since we're stateless and use a Bearer token that the
     *   client must deliberately attach to every request (not
     *   auto-sent by the browser like a cookie), CSRF doesn't apply
     *   the same way here.
     * - SessionCreationPolicy.STATELESS: Spring Security will never
     *   create or use an HttpSession. Every request is independently
     *   authenticated from its JWT, every time.
     * - Public endpoints: registration and login must be reachable
     *   WITHOUT a token (you don't have one yet!).
     * - Everything else requires a valid, authenticated request.
     * - Our JwtAuthenticationFilter runs BEFORE Spring Security's own
     *   UsernamePasswordAuthenticationFilter, so by the time Spring
     *   Security decides whether to allow/deny a request, it already
     *   knows (from our filter) who's making it, if anyone.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/register",
                                "/api/v1/auth/login"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
