package com.nexora.exception;

import com.nexora.auth.exception.AccountNotActiveException;
import com.nexora.auth.exception.InvalidCredentialsException;
import com.nexora.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * WHAT IS GLOBAL EXCEPTION HANDLING?
 * Instead of writing try/catch in every single controller method,
 * @RestControllerAdvice lets us define ONE place that catches
 * exceptions thrown ANYWHERE in the app and converts them into
 * clean, consistent JSON error responses with the right HTTP status code.
 *
 * This keeps controllers simple (no error-formatting logic) and makes
 * sure the frontend always gets errors in the same predictable shape.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Triggered automatically when @Valid fails on a request body
     * (e.g. missing name, invalid email format, short password).
     * Returns 400 BAD_REQUEST with a field-by-field breakdown.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        ApiResponse<Map<String, String>> response =
                ApiResponse.error("Validation failed");
        // Attach field errors as the "data" payload so the client can
        // show inline error messages next to each form field.
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, "Validation failed", fieldErrors, response.getTimestamp()));
    }

    /**
     * Triggered when someone tries to register with an email that
     * already exists. Returns 409 CONFLICT.
     */
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiResponse<Object>> handleDuplicateEmail(DuplicateEmailException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Triggered when a requested resource (e.g. user by id) doesn't exist.
     * Returns 404 NOT_FOUND.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Triggered on a failed login: wrong password or unknown email.
     * Returns 401 UNAUTHORIZED with a deliberately generic message
     * (see InvalidCredentialsException's javadoc for why).
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Triggered when correct credentials are supplied but the account
     * status isn't ACTIVE (e.g. SUSPENDED). Returns 403 FORBIDDEN —
     * the identity was proven, but access is still refused.
     */
    @ExceptionHandler(AccountNotActiveException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccountNotActive(AccountNotActiveException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Catch-all safety net for any exception we didn't anticipate.
     * Returns 500 INTERNAL_SERVER_ERROR instead of leaking a raw
     * stack trace to the client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Something went wrong: " + ex.getMessage()));
    }
}
