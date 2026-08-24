package com.nexora.exception;

import com.nexora.auth.exception.AccountNotActiveException;
import com.nexora.auth.exception.InvalidCredentialsException;
import com.nexora.category.exception.CategoryInUseException;
import com.nexora.category.exception.DuplicateCategoryException;
import com.nexora.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GLOBAL EXCEPTION HANDLING
 *
 * Instead of writing try/catch blocks in every controller,
 * @RestControllerAdvice provides one central place to convert
 * application exceptions into consistent JSON responses.
 *
 * This keeps controllers clean and ensures the API returns
 * predictable HTTP status codes and response structures.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles @Valid request-body validation failures.
     *
     * Example:
     * - Missing required field
     * - Invalid email
     * - Invalid password length
     *
     * HTTP 400 BAD_REQUEST
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(
                    error.getField(),
                    error.getDefaultMessage()
            );
        }

        ApiResponse<Map<String, String>> response =
                ApiResponse.error("Validation failed");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(
                        false,
                        "Validation failed",
                        fieldErrors,
                        response.getTimestamp()
                ));
    }

    /**
     * Handles duplicate email registration attempts.
     *
     * HTTP 409 CONFLICT
     */
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiResponse<Object>> handleDuplicateEmail(
            DuplicateEmailException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles requests for resources that do not exist.
     *
     * HTTP 404 NOT_FOUND
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFound(
            ResourceNotFoundException ex) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles failed login attempts.
     *
     * HTTP 401 UNAUTHORIZED
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidCredentials(
            InvalidCredentialsException ex) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles authenticated users whose account is not active.
     *
     * HTTP 403 FORBIDDEN
     */
    @ExceptionHandler(AccountNotActiveException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccountNotActive(
            AccountNotActiveException ex) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles duplicate category creation/rename attempts.
     *
     * HTTP 409 CONFLICT
     */
    @ExceptionHandler(DuplicateCategoryException.class)
    public ResponseEntity<ApiResponse<Object>> handleDuplicateCategory(
            DuplicateCategoryException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles attempts to delete a category that is still
     * associated with another resource.
     *
     * HTTP 409 CONFLICT
     */
    @ExceptionHandler(CategoryInUseException.class)
    public ResponseEntity<ApiResponse<Object>> handleCategoryInUse(
            CategoryInUseException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles application-level ownership/permission failures.
     *
     * Example:
     * A STORE_OWNER tries to modify another owner's store.
     *
     * HTTP 403 FORBIDDEN
     */
    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<ApiResponse<Object>> handleForbiddenOperation(
            ForbiddenOperationException ex) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles Spring Security authorization failures.
     *
     * This is particularly important for Phase 4 @PreAuthorize
     * role-based authorization.
     *
     * Example:
     * CUSTOMER -> ADMIN-only endpoint
     *
     * Spring Security throws AccessDeniedException.
     *
     * Without this handler, the exception can reach the generic
     * Exception handler and incorrectly become HTTP 500.
     *
     * Correct API behavior:
     *
     * Authenticated but insufficient permissions -> HTTP 403
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(
            AccessDeniedException ex) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied"));
    }

    /**
     * Catch-all handler for unexpected exceptions.
     *
     * HTTP 500 INTERNAL_SERVER_ERROR
     *
     * This handler remains last so that specific exceptions above
     * can be converted to their correct HTTP status codes first.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(
            Exception ex) {

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "Something went wrong: " + ex.getMessage()
                ));
    }
}