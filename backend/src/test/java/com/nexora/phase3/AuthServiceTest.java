package com.nexora.phase3;

import com.nexora.auth.dto.AuthResponse;
import com.nexora.auth.dto.LoginRequest;
import com.nexora.auth.exception.AccountNotActiveException;
import com.nexora.auth.exception.InvalidCredentialsException;
import com.nexora.auth.security.JwtService;
import com.nexora.auth.security.UserPrincipal;
import com.nexora.auth.service.AuthService;
import com.nexora.role.entity.Role;
import com.nexora.role.entity.RoleName;
import com.nexora.user.entity.User;
import com.nexora.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * WHY MOCK AuthenticationManager INSTEAD OF TESTING THROUGH REAL
 * SPRING SECURITY HERE?
 * This is a unit test for AuthService's OWN logic: does it correctly
 * translate the AuthenticationManager's outcome into the right
 * AuthResponse or the right exception? The actual password-comparison
 * mechanics (BCrypt matching) are Spring Security's job and are
 * exercised instead in AuthControllerIntegrationTest, which runs the
 * real filter chain end-to-end.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService unit tests")
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    private AuthService authService;

    private User activeUser;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        JwtService jwtService = new JwtService(
                "test-only-secret-not-used-anywhere-real-abcdefghijklmnopqrstuvwxyz123456",
                3_600_000L
        );

        authService = new AuthService(authenticationManager, userRepository, jwtService);
        // access-token-expiration-ms is normally injected by Spring via
        // @Value; since we're constructing AuthService manually in this
        // unit test (no Spring context), we set it directly.
        ReflectionTestUtils.setField(authService, "accessTokenExpirationMs", 3_600_000L);

        Role customerRole = Role.builder().id(1L).name(RoleName.CUSTOMER).build();
        activeUser = User.builder()
                .id(1L)
                .name("Aditi Sharma")
                .email("aditi@example.com")
                .password("bcrypt-hashed-value")
                .status("ACTIVE")
                .roles(Set.of(customerRole))
                .build();

        loginRequest = new LoginRequest("aditi@example.com", "SecurePass123");
    }

    @Test
    @DisplayName("Should return a valid AuthResponse with a token on correct credentials")
    void shouldLoginSuccessfullyWithCorrectCredentials() {
        UserPrincipal principal = new UserPrincipal(activeUser);
        UsernamePasswordAuthenticationToken authResult =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(authResult);

        AuthResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getEmail()).isEqualTo("aditi@example.com");
        assertThat(response.getRoles()).containsExactly("CUSTOMER");
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException on wrong password")
    void shouldThrowOnWrongPassword() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException (not a different message) for an unknown email")
    void shouldThrowSameGenericErrorForUnknownEmail() {
        // Spring Security's DaoAuthenticationProvider throws
        // BadCredentialsException for BOTH "no such user" and "wrong
        // password" by default (it deliberately hides which one),
        // so AuthService sees the same exception type either way.
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        LoginRequest unknownEmailRequest = new LoginRequest("nobody@example.com", "whatever123");

        assertThatThrownBy(() -> authService.login(unknownEmailRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password"); // same message as wrong-password case
    }

    @Test
    @DisplayName("Should throw AccountNotActiveException when the account is disabled/suspended")
    void shouldThrowWhenAccountIsSuspended() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new DisabledException("User is disabled"));
        when(userRepository.findByEmail("aditi@example.com"))
                .thenReturn(Optional.of(
                        User.builder()
                                .id(1L).name("Aditi Sharma").email("aditi@example.com")
                                .password("hashed").status("SUSPENDED")
                                .roles(Set.of(Role.builder().id(1L).name(RoleName.CUSTOMER).build()))
                                .build()
                ));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(AccountNotActiveException.class)
                .hasMessageContaining("SUSPENDED");
    }
}
