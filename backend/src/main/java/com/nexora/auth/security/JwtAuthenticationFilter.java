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
 *   1. Look for an "Authorization: Bearer <token>" header or "?token=<token>" query param.
 *   2. If present, extract the email from the token.
 *   3. Load the matching user from the database (via UserDetailsService).
 *   4. Verify the token's signature, expiry, and that it matches that
 *      user (see JwtService.isTokenValid).
 *   5. If everything checks out, tell Spring Security "this request IS
 *      authenticated as this user" by populating the SecurityContext.
 *   6. Let the request continue down the filter chain either way.
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
        String token = null;

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            token = authHeader.substring(BEARER_PREFIX.length());
        } else if (request.getParameter("token") != null && !request.getParameter("token").isBlank()) {
            // Allows EventSource SSE and WebSocket query parameter authentication
            token = request.getParameter("token");
        }

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String email = jwtService.extractEmail(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserPrincipal principal =
                        (UserPrincipal) userDetailsService.loadUserByUsername(email);

                if (jwtService.isTokenValid(token, principal)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    principal,
                                    null,
                                    principal.getAuthorities()
                            );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception ignored) {
            // Malformed token, expired token, etc. Let filter chain proceed.
        }

        filterChain.doFilter(request, response);
    }
}
