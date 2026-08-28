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
import com.nexora.payment.exception.InvalidPaymentSignatureException;
import com.nexora.payment.exception.InvalidPaymentStateException;
import com.nexora.payment.gateway.PaymentGatewayException;
import com.nexora.product.exception.InvalidProductDataException;
import com.nexora.product.exception.ProductInUseException;
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
 * GLOBAL EXCEPTION HANDLING.
 *
 * Instead of writing try/catch blocks in every controller,
 * @RestControllerAdvice provides one centralized location that converts
 * application exceptions into consistent JSON responses with the
 * appropriate HTTP status code.
 *
 * This keeps controllers clean and makes the API error format predictable.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Triggered automatically when @Valid fails on a request body.
     *
     * Example:
     * - missing required field
     * - invalid email
     * - password too short
     *
     * Returns 400 BAD_REQUEST with field-level validation errors.
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
                .body(
                        new ApiResponse<>(
                                false,
                                "Validation failed",
                                fieldErrors,
                                response.getTimestamp()
                        )
                );
    }

    /**
     * Triggered when someone tries to register using an email
     * that already exists.
     *
     * Returns 409 CONFLICT.
     */
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiResponse<Object>> handleDuplicateEmail(
            DuplicateEmailException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Triggered when a requested resource does not exist.
     *
     * Returns 404 NOT_FOUND.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFound(
            ResourceNotFoundException ex) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Triggered when login credentials are invalid.
     *
     * Returns 401 UNAUTHORIZED.
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidCredentials(
            InvalidCredentialsException ex) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Triggered when a user's account is not ACTIVE.
     *
     * Returns 403 FORBIDDEN.
     */
    @ExceptionHandler(AccountNotActiveException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccountNotActive(
            AccountNotActiveException ex) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Triggered when an ADMIN tries to create or rename a category
     * using a name that already exists.
     *
     * Returns 409 CONFLICT.
     */
    @ExceptionHandler(DuplicateCategoryException.class)
    public ResponseEntity<ApiResponse<Object>> handleDuplicateCategory(
            DuplicateCategoryException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Triggered when trying to delete a category that is still
     * attached to a store or product.
     *
     * Returns 409 CONFLICT.
     */
    @ExceptionHandler(CategoryInUseException.class)
    public ResponseEntity<ApiResponse<Object>> handleCategoryInUse(
            CategoryInUseException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * SPRING SECURITY METHOD-AUTHORIZATION FAILURE.
     *
     * This exception is thrown when an authenticated user has a valid
     * identity but does not have the role required by @PreAuthorize.
     *
     * Example:
     *
     * CUSTOMER
     *     ↓
     * @PreAuthorize("hasAnyRole('STORE_OWNER','ADMIN')")
     *     ↓
     * AccessDeniedException
     *     ↓
     * HTTP 403 FORBIDDEN
     *
     * This is different from authentication failure:
     *
     * 401 = user is not authenticated.
     * 403 = user is authenticated but not authorized.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(
            AccessDeniedException ex) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(
                        ApiResponse.error(
                                "Access denied. Your account does not have permission to perform this action."
                        )
                );
    }

    /**
     * RESOURCE-LEVEL AUTHORIZATION FAILURE.
     *
     * This is used when the user has an appropriate role but does not
     * own the particular resource.
     *
     * Example:
     *
     * STORE_OWNER A
     *     ↓
     * tries to modify
     *     ↓
     * Store owned by STORE_OWNER B
     *
     * Returns 403 FORBIDDEN.
     */
    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<ApiResponse<Object>> handleForbiddenOperation(
            ForbiddenOperationException ex) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Triggered when product data passes field validation but is
     * logically inconsistent.
     *
     * Example:
     * discount > price
     *
     * Returns 400 BAD_REQUEST.
     */
    @ExceptionHandler(InvalidProductDataException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidProductData(
            InvalidProductDataException ex) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Triggered when a stock operation requests more inventory
     * than is currently available.
     *
     * Returns 409 CONFLICT.
     */
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiResponse<Object>> handleInsufficientStock(
            InsufficientStockException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Triggered when attempting to add or increase a product in a
     * cart while the product is unavailable.
     *
     * Returns 409 CONFLICT.
     */
    @ExceptionHandler(ProductUnavailableException.class)
    public ResponseEntity<ApiResponse<Object>> handleProductUnavailable(
            ProductUnavailableException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Triggered when attempting checkout with an empty cart.
     *
     * Returns 400 BAD_REQUEST.
     */
    @ExceptionHandler(EmptyCartException.class)
    public ResponseEntity<ApiResponse<Object>> handleEmptyCart(
            EmptyCartException ex) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Triggered when an invalid order-status transition is attempted.
     *
     * Example:
     *
     * DELIVERED -> PREPARING
     *
     * Returns 409 CONFLICT.
     */
    @ExceptionHandler(InvalidOrderStatusTransitionException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidOrderStatusTransition(
            InvalidOrderStatusTransitionException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Triggered when attempting to delete a product that is referenced
     * by an existing order.
     *
     * Returns 409 CONFLICT.
     */
    @ExceptionHandler(ProductInUseException.class)
    public ResponseEntity<ApiResponse<Object>> handleProductInUse(
            ProductInUseException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Triggered when a payment operation is not valid for the order's
     * current payment state.
     *
     * Example:
     * attempting to initiate payment for an already PAID order.
     *
     * Returns 409 CONFLICT.
     */
    @ExceptionHandler(InvalidPaymentStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidPaymentState(
            InvalidPaymentStateException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Triggered when communication with Razorpay fails.
     *
     * Example:
     * - network failure
     * - invalid gateway credentials
     * - gateway-side error
     *
     * Returns 502 BAD_GATEWAY.
     */
    @ExceptionHandler(PaymentGatewayException.class)
    public ResponseEntity<ApiResponse<Object>> handlePaymentGatewayException(
            PaymentGatewayException ex) {

        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Triggered when a Razorpay payment or webhook signature fails
     * verification.
     *
     * Returns 400 BAD_REQUEST.
     */
    @ExceptionHandler(InvalidPaymentSignatureException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidPaymentSignature(
            InvalidPaymentSignatureException ex) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Catch-all safety net.
     *
     * Any unexpected exception that does not have a dedicated handler
     * is converted into HTTP 500 rather than exposing a raw stack trace.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(
            Exception ex) {

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        ApiResponse.error(
                                "Something went wrong: " + ex.getMessage()
                        )
                );
    }
}