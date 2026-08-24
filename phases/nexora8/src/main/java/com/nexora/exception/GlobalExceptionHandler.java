package com.nexora.exception;

import com.nexora.auth.exception.AccountNotActiveException;
import com.nexora.auth.exception.InvalidCredentialsException;
import com.nexora.cart.exception.ProductUnavailableException;
import com.nexora.category.exception.CategoryInUseException;
import com.nexora.category.exception.DuplicateCategoryException;
import com.nexora.common.ApiResponse;
import com.nexora.inventory.exception.InsufficientStockException;
import com.nexora.order.exception.EmptyCartException;
import com.nexora.order.exception.InvalidOrderStatusTransitionException;
import com.nexora.product.exception.InvalidProductDataException;
import com.nexora.product.exception.ProductInUseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handling for the Nexora application.
 *
 * All controllers can throw application-specific exceptions without
 * having to repeat try/catch blocks. This class converts those
 * exceptions into consistent API responses with the appropriate
 * HTTP status code.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles @Valid request-body validation failures.
     *
     * Returns 400 BAD_REQUEST with field-level errors.
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
     * Duplicate email during registration.
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
     * Requested resource does not exist.
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
     * Login failed because credentials are invalid.
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
     * User is authenticated but account is not active.
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
     * Category name already exists.
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
     * Category cannot be deleted because it is still in use.
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
     * Application-level ownership/permission failure.
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
     * Product data is logically invalid.
     *
     * Example:
     * discount greater than price.
     *
     * HTTP 400 BAD_REQUEST
     */
    @ExceptionHandler(InvalidProductDataException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidProductData(
            InvalidProductDataException ex) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Requested stock is greater than available stock.
     *
     * HTTP 409 CONFLICT
     */
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiResponse<Object>> handleInsufficientStock(
            InsufficientStockException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Product is unavailable for adding to cart.
     *
     * HTTP 409 CONFLICT
     */
    @ExceptionHandler(ProductUnavailableException.class)
    public ResponseEntity<ApiResponse<Object>> handleProductUnavailable(
            ProductUnavailableException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Checkout attempted with an empty cart.
     *
     * HTTP 400 BAD_REQUEST
     */
    @ExceptionHandler(EmptyCartException.class)
    public ResponseEntity<ApiResponse<Object>> handleEmptyCart(
            EmptyCartException ex) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Invalid order status transition.
     *
     * Example:
     * PREPARING -> CREATED
     *
     * HTTP 409 CONFLICT
     */
    @ExceptionHandler(InvalidOrderStatusTransitionException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidOrderStatusTransition(
            InvalidOrderStatusTransitionException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Product cannot be deleted because an existing order references it.
     *
     * HTTP 409 CONFLICT
     */
    @ExceptionHandler(ProductInUseException.class)
    public ResponseEntity<ApiResponse<Object>> handleProductInUse(
            ProductInUseException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Spring Security 6 method-security denial.
     *
     * @PreAuthorize failures can arrive as AuthorizationDeniedException.
     *
     * HTTP 403 FORBIDDEN
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthorizationDenied(
            AuthorizationDeniedException ex) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access Denied"));
    }

    /**
     * Final safety-net handler.
     *
     * Only genuinely unexpected exceptions should reach this method.
     *
     * HTTP 500 INTERNAL_SERVER_ERROR
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