package com.nexora.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * WHAT THIS FILTER DOES, ON EVERY SINGLE REQUEST:
 *   1. Look for an "Authorization: Bearer <token>" header.
 *   2. If present, extract the email from the token.
 *   3. Load the matching user from the database (via UserDetailsService).
 *   4. Verify the token's signature, expiry, and that it matches that
 *      user (see JwtService.isTokenValid).
 *   5. If everything checks out, tell Spring Security "this request IS
 *      authenticated as this user" by populating the SecurityContext.
 *   6. Let the request continue down the filter chain either way — if
 *      there's no valid token, we simply don't authenticate; whether
 *      that request is then ALLOWED or REJECTED (401/403) is decided
 *      later by SecurityConfig's endpoint rules, not by this filter.
 *
 * OncePerRequestFilter guarantees this logic runs exactly once per
 * request, even in environments with internal request forwarding.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(AUTH_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            // No bearer token at all — just move on. Public endpoints
            // (like /api/v1/auth/login) don't need one; protected ones
            // will be rejected downstream by SecurityConfig.
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            String email = jwtService.extractEmail(token);

            // Only attempt authentication if there ISN'T already an
            // authenticated principal in this request's context —
            // avoids redundant work if something upstream already set it.
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserPrincipal principal =
                        (UserPrincipal) userDetailsService.loadUserByUsername(email);

                if (jwtService.isTokenValid(token, principal)) {

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    principal,
                                    null, // credentials not needed post-authentication
                                    principal.getAuthorities()
                            );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception ex) {
            // Any problem with the token (expired, malformed, signature
            // mismatch, user no longer exists) simply means: don't
            // authenticate this request. We deliberately do NOT throw
            // here — that would produce a raw 500 error instead of a
            // clean 401/403 from Spring Security's normal handling.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
