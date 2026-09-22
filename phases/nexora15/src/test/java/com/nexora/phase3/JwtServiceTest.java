package com.nexora.phase3;

import com.nexora.auth.security.JwtService;
import com.nexora.auth.security.UserPrincipal;
import com.nexora.role.entity.Role;
import com.nexora.role.entity.RoleName;
import com.nexora.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for JwtService — no Spring context needed at all,
 * we just construct it directly with a known test secret and check
 * its behavior in isolation. This is the fastest possible way to
 * verify token generation/validation logic.
 */
@DisplayName("JwtService unit tests")
class JwtServiceTest {

    // A 64+ char test secret — long enough for HMAC-SHA256/512 signing.
    private static final String TEST_SECRET =
            "test-only-secret-not-used-anywhere-real-abcdefghijklmnopqrstuvwxyz123456";

    private JwtService jwtService;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        // 1 hour access token expiry for these tests
        jwtService = new JwtService(TEST_SECRET, 3_600_000L);

        Role customerRole = Role.builder().id(1L).name(RoleName.CUSTOMER).build();
        User user = User.builder()
                .id(42L)
                .name("Test User")
                .email("test.user@example.com")
                .password("hashed")
                .status("ACTIVE")
                .roles(Set.of(customerRole))
                .build();

        principal = new UserPrincipal(user);
    }

    @Test
    @DisplayName("Should generate a non-empty token with 3 dot-separated parts")
    void shouldGenerateValidTokenStructure() {
        String token = jwtService.generateToken(principal);

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
    }

    @Test
    @DisplayName("Should correctly extract the email (subject) from a generated token")
    void shouldExtractEmailFromToken() {
        String token = jwtService.generateToken(principal);

        String extractedEmail = jwtService.extractEmail(token);

        assertThat(extractedEmail).isEqualTo("test.user@example.com");
    }

    @Test
    @DisplayName("Should correctly extract the user id from a generated token")
    void shouldExtractUserIdFromToken() {
        String token = jwtService.generateToken(principal);

        Long extractedUserId = jwtService.extractUserId(token);

        assertThat(extractedUserId).isEqualTo(42L);
    }

    @Test
    @DisplayName("Should correctly extract roles from a generated token")
    void shouldExtractRolesFromToken() {
        String token = jwtService.generateToken(principal);

        assertThat(jwtService.extractRoles(token)).containsExactly("CUSTOMER");
    }

    @Test
    @DisplayName("A freshly generated token should be valid for the user it was issued to")
    void freshTokenShouldBeValid() {
        String token = jwtService.generateToken(principal);

        assertThat(jwtService.isTokenValid(token, principal)).isTrue();
    }

    @Test
    @DisplayName("A token should NOT be valid for a different user")
    void tokenShouldNotBeValidForDifferentUser() {
        String token = jwtService.generateToken(principal);

        Role role = Role.builder().id(1L).name(RoleName.CUSTOMER).build();
        User differentUser = User.builder()
                .id(99L)
                .name("Someone Else")
                .email("someone.else@example.com")
                .password("hashed")
                .status("ACTIVE")
                .roles(Set.of(role))
                .build();
        UserPrincipal differentPrincipal = new UserPrincipal(differentUser);

        assertThat(jwtService.isTokenValid(token, differentPrincipal)).isFalse();
    }

    @Test
    @DisplayName("An already-expired token should be rejected as invalid")
    void expiredTokenShouldBeInvalid() {
        // Build a JwtService whose tokens expire immediately (-1 ms),
        // so any token it issues is already expired by the time we check it.
        JwtService shortLivedJwtService = new JwtService(TEST_SECRET, -1L);

        String token = shortLivedJwtService.generateToken(principal);

        assertThat(shortLivedJwtService.isTokenValid(token, principal)).isFalse();
    }

    @Test
    @DisplayName("A token signed with a different secret should be rejected")
    void tokenSignedWithDifferentSecretShouldBeRejected() {
        JwtService otherJwtService = new JwtService(
                "a-completely-different-64-char-test-secret-zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz",
                3_600_000L
        );

        String tokenFromOtherService = otherJwtService.generateToken(principal);

        // jwtService (our original, different secret) should refuse to
        // trust a token it didn't sign.
        assertThat(jwtService.isTokenValid(tokenFromOtherService, principal)).isFalse();
    }
}
