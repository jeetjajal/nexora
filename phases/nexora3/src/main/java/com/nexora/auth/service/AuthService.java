package com.nexora.auth.service;

import com.nexora.auth.JwtService;
import com.nexora.auth.dto.AuthResponse;
import com.nexora.auth.dto.LoginRequest;
import com.nexora.auth.exception.AccountNotActiveException;
import com.nexora.auth.exception.InvalidCredentialsException;
import com.nexora.exception.ResourceNotFoundException;
import com.nexora.user.dto.UserResponse;
import com.nexora.user.entity.User;
import com.nexora.user.mapper.UserMapper;
import com.nexora.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new AccountNotActiveException(
                    "Account is not active"
            );
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()
                    )
            );
        } catch (BadCredentialsException ex) {
            throw new InvalidCredentialsException();
        }

        String accessToken =
                jwtService.generateAccessToken(user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(3600)
                .build();
    }

    public UserResponse getCurrentUser(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with email: " + email
                        ));

        return UserMapper.toResponse(user);
    }
}