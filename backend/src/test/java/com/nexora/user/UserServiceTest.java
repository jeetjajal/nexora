package com.nexora.user;

import com.nexora.exception.DuplicateEmailException;
import com.nexora.role.entity.Role;
import com.nexora.role.entity.RoleName;
import com.nexora.role.repository.RoleRepository;
import com.nexora.user.dto.RegisterRequest;
import com.nexora.user.dto.UserResponse;
import com.nexora.user.entity.User;
import com.nexora.user.repository.UserRepository;
import com.nexora.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * WHAT IS A UNIT TEST?
 * A unit test checks ONE small piece of logic (here, UserService)
 * in isolation, without needing a real MySQL database or a running
 * server. We achieve isolation with Mockito, which lets us create a
 * "fake" UserRepository (@Mock) that returns exactly what we tell it
 * to, so we can test UserService's behavior on its own.
 *
 * These are fast (milliseconds) and should be run constantly while
 * developing. Slower, full-stack tests (with a real/test database)
 * come later in Phase 14.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService unit tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserService userService;

    private RegisterRequest registerRequest;
    private Role customerRole;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest(
                "Aditi Sharma",
                "aditi@example.com",
                "SecurePass123",
                "9876543210"
        );

        customerRole = Role.builder().id(1L).name(RoleName.CUSTOMER).build();
    }

    @Test
    @DisplayName("Should register a new user successfully when email is not taken")
    void shouldRegisterUserSuccessfully() {
        // Arrange: pretend the email doesn't exist yet, pretend the
        // CUSTOMER role is already seeded (DataSeeder's job in real
        // life), and pretend save() returns a fully-populated User.
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(roleRepository.findByName(RoleName.CUSTOMER)).thenReturn(java.util.Optional.of(customerRole));

        User savedUser = User.builder()
                .id(1L)
                .name(registerRequest.getName())
                .email(registerRequest.getEmail())
                .password("hashed-password")
                .phone(registerRequest.getPhone())
                .status("ACTIVE")
                .roles(Set.of(customerRole))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        UserResponse response = userService.register(registerRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("aditi@example.com");
        assertThat(response.getRoles()).containsExactly("CUSTOMER");

        // Verify save() was actually called exactly once with a User object
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw DuplicateEmailException when email is already registered")
    void shouldThrowWhenEmailAlreadyExists() {
        // Arrange
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> userService.register(registerRequest))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining(registerRequest.getEmail());

        // save() must NEVER be called if the email is already taken
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should never save the plain-text password")
    void shouldHashPasswordBeforeSaving() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName(RoleName.CUSTOMER)).thenReturn(java.util.Optional.of(customerRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userService.register(registerRequest);

        // Assert: capture what was actually passed to save() and check
        // the password stored is NOT the raw password the user typed.
        verify(userRepository).save(argThat(user ->
                !user.getPassword().equals(registerRequest.getPassword())
        ));
    }
}
