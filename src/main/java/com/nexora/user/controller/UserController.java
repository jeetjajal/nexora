package com.nexora.user.controller;

import com.nexora.common.ApiResponse;
import com.nexora.user.dto.RegisterRequest;
import com.nexora.user.dto.UserResponse;
import com.nexora.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * CONTROLLER = the "front door" of the API.
 * Its ONLY job is to:
 *   1. Receive the HTTP request
 *   2. Validate the shape of the input (via @Valid)
 *   3. Call the Service layer to do the real work
 *   4. Wrap the result in a standard ApiResponse and return it
 *
 * No business logic (like checking duplicate emails or hashing
 * passwords) belongs here — that all lives in UserService.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Register a new Nexora customer account.
     *
     * Sample request (POST /api/v1/auth/register):
     * {
     *   "name": "Aditi Sharma",
     *   "email": "aditi@example.com",
     *   "password": "SecurePass123",
     *   "phone": "9876543210"
     * }
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        UserResponse userResponse = userService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", userResponse));
    }

    /**
     * Fetch a user by ID. Useful right now for manually verifying that
     * registration actually persisted data to MySQL.
     * Full "my profile" / auth-protected endpoints arrive in Phase 3.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        UserResponse userResponse = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User fetched successfully", userResponse));
    }
}
