package com.nexora.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * WHAT IS A DTO?
 * DTO = Data Transfer Object. It's the shape of data that moves between
 * the client (frontend / Postman / mobile app) and our API.
 *
 * We NEVER expose the JPA @Entity (User) directly in a controller because:
 *   1. It would leak internal fields (like the hashed password) in responses.
 *   2. It tightly couples our public API to our database schema — if we
 *      rename a database column, we don't want every API consumer to break.
 *
 * This DTO represents exactly what a client must send to register.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters long")
    private String password;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be exactly 10 digits")
    private String phone;
}
