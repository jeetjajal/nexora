package com.nexora.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * What a client must send to POST /api/v1/auth/login.
 * Deliberately minimal: just the two credentials. We don't validate
 * password length/strength HERE — that belongs to registration.
 * A login attempt with a "too short" password should still be
 * evaluated (and correctly rejected as wrong) rather than blocked at
 * the DTO-validation layer, which would leak information about
 * password policy to anyone probing the login endpoint.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
