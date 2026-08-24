package com.nexora.auth.controller;

import com.nexora.auth.dto.AuthResponse;
import com.nexora.auth.dto.LoginRequest;
import com.nexora.auth.service.AuthService;
import com.nexora.common.ApiResponse;
import com.nexora.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse authResponse = authService.login(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Login successful",
                        authResponse
                )
        );
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(
            Authentication authentication) {

        String email = authentication.getName();

        UserResponse user = authService.getCurrentUser(email);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Current user fetched successfully",
                        user
                )
        );
    }

    @GetMapping("/admin-test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> adminTest() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Admin authorization successful",
                        "ADMIN endpoint is accessible"
                )
        );
    }
}