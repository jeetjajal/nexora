package com.nexora.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * STANDARD API RESPONSE ENVELOPE
 *
 * Every Nexora endpoint returns the same "shape" of JSON, whether it
 * succeeds or fails. This makes the frontend's job predictable:
 *
 * Success example:
 * {
 *   "success": true,
 *   "message": "User registered successfully",
 *   "data": { "id": 1, "name": "Aditi Sharma", ... },
 *   "timestamp": "2026-08-15T10:15:30"
 * }
 *
 * Error example:
 * {
 *   "success": false,
 *   "message": "Email already registered",
 *   "data": null,
 *   "timestamp": "2026-08-15T10:15:30"
 * }
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, LocalDateTime.now());
    }
}
