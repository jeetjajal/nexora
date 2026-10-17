package com.nexora.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * WHAT IS A JWT?
 * A JSON Web Token is a compact, digitally SIGNED string with three
 * dot-separated parts: header.payload.signature. Once we sign it with
 * a secret key, anyone who has the token can READ the payload (it's
 * just Base64, not encrypted) but CANNOT modify it without invalidating
 * the signature — so the server can trust a JWT it receives back, as
 * long as the signature checks out.
 *
 * WHY USE JWT INSTEAD OF SERVER-SIDE SESSIONS?
 * With sessions, the server has to remember every logged-in user in
 * memory or a shared store (like Redis) and look it up on every
 * request. A JWT is "stateless" — all the information Spring Security
 * needs (who is this user, what roles do they have) is embedded IN the
 * token itself, so the server can verify it instantly without any
 * database or cache lookup. This scales horizontally very easily:
 * any server instance holding the same secret key can validate any
 * token, with zero shared session storage.
 *
 * WHAT WE STORE INSIDE THE TOKEN (the "claims"):
 *   - subject (sub)   -> the user's email
 *   - userId (custom) -> the user's numeric id, so we don't need a
 *                         database lookup just to know who's calling
 *   - roles (custom)  -> role names, so authorization checks don't
 *                         need a database lookup either
 *   - issuedAt (iat)  -> when the token was created
 *   - expiration (exp)-> when the token stops being valid
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenExpirationMs;

    public JwtService(
            @Value("${nexora.jwt.secret}") String secret,
            @Value("${nexora.jwt.access-token-expiration-ms}") long accessTokenExpirationMs) {

        // The secret string from application.properties is turned into
        // a proper cryptographic key here. HMAC-SHA algorithms (which
        // jjwt uses by default for a plain secret) require the key to
        // be long enough — that's why the dev default secret in
        // application.properties is a long string, not a short word.
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    /**
     * Creates a signed JWT for a freshly authenticated user.
     */
    public String generateToken(UserPrincipal principal) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpirationMs);

        List<String> roleNames = principal.getUser().getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());

        return Jwts.builder()
                .subject(principal.getUsername()) // email
                .claim("userId", principal.getId())
                .claim("roles", roleNames)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Extracts the email (subject) from a token WITHOUT checking
     * whether the caller is authorized to do anything — just reads
     * the claim. Signature is still verified during parsing, so a
     * tampered token will still throw before we get any claim back.
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("userId", Long.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("roles", List.class);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Full validation: signature is correct AND token isn't expired
     * AND the subject (email) matches the user we loaded from the
     * database via UserDetailsService. Comparing against a freshly
     * loaded UserDetails (rather than trusting the token blindly)
     * protects against a token that's technically still "valid" but
     * whose underlying account was deactivated/suspended after issue.
     */
    public boolean isTokenValid(String token, UserPrincipal principal) {
        try {
            String email = extractEmail(token);
            return email.equals(principal.getUsername())
                    && !isTokenExpired(token)
                    && principal.isEnabled()
                    && principal.isAccountNonLocked();
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parses and verifies the token's signature. If the token was
     * tampered with, or signed with a different key, or malformed,
     * this throws a JwtException — which JwtAuthenticationFilter
     * catches and turns into "just don't authenticate this request"
     * rather than crashing the whole application.
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            // Still return the claims of an expired token — callers
            // like isTokenExpired() need to read the expiration date
            // itself, which requires the payload even though it's expired.
            return ex.getClaims();
        } catch (SignatureException ex) {
            throw new JwtException("Invalid JWT signature", ex);
        }
    }
}
