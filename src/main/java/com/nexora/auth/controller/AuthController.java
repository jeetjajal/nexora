package com.nexora.auth.controller;

import com.nexora.auth.dto.AuthResponse;
import com.nexora.auth.dto.LoginRequest;
import com.nexora.auth.security.UserPrincipal;
import com.nexora.auth.service.AuthService;
import com.nexora.common.ApiResponse;
import com.nexora.user.dto.UserResponse;
import com.nexora.user.mapper.UserMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login lives here, separate from UserController's /register (Phase 1/2).
 * Both sit under /api/v1/auth/* to match the architecture diagram —
 * they're conceptually one "authentication" surface even though
 * registration logic is owned by UserService and login logic by
 * AuthService.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Sample request:
     * {
     *   "email": "aditi@example.com",
     *   "password": "SecurePass123"
     * }
     *
     * Sample success response:
     * {
     *   "success": true,
     *   "message": "Login successful",
     *   "data": {
     *     "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
     *     "tokenType": "Bearer",
     *     "expiresInMs": 3600000,
     *     "userId": 1,
     *     "name": "Aditi Sharma",
     *     "email": "aditi@example.com",
     *     "roles": ["CUSTOMER"]
     *   }
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    /**
     * PROTECTED endpoint — requires a valid "Authorization: Bearer <token>"
     * header (enforced by SecurityConfig + JwtAuthenticationFilter).
     * Demonstrates extracting the authenticated user's identity from
     * the JWT via Spring Security's @AuthenticationPrincipal, with NO
     * extra database lookup needed beyond what the filter already did.
     *
     * Useful for a frontend to say "who's currently logged in?" and to
     * manually verify end-to-end that a token actually authenticates.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(@AuthenticationPrincipal UserPrincipal principal) {
        UserResponse response = UserMapper.toResponse(principal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Authenticated user fetched successfully", response));
    }
}
