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
 * GLOBAL EXCEPTION HANDLER
 *
 * This class provides one centralized place for converting application
 * exceptions into consistent JSON responses with appropriate HTTP status
 * codes.
 *
 * This keeps controller classes clean and prevents every controller from
 * needing its own try/catch and error-response logic.
 *
 * IMPORTANT SECURITY HANDLING:
 *
 * There are TWO different kinds of authorization failures in Nexora:
 *
 * 1. ForbiddenOperationException
 *    - Thrown manually from service-layer business authorization checks.
 *    - Example: a STORE_OWNER attempts to modify another owner's store.
 *    - Must return HTTP 403 FORBIDDEN.
 *
 * 2. Spring Security AccessDeniedException
 *    - Thrown when an authenticated user's role is not permitted to access
 *      an endpoint protected by Spring Security / @PreAuthorize.
 *    - Example: CUSTOMER attempts to call an ADMIN-only endpoint.
 *    - Must also return HTTP 403 FORBIDDEN.
 *
 * Without the explicit AccessDeniedException handler below, the exception
 * can fall through to the generic Exception handler and incorrectly become
 * HTTP 500 INTERNAL_SERVER_ERROR.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Triggered automatically when @Valid fails on a request body.
     *
     * Example:
     * - missing name
     * - invalid email
     * - password too short
     *
     * Returns:
     * HTTP 400 BAD_REQUEST
     *
     * The response contains field-by-field validation errors.
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
                new ApiResponse<>(
                        false,
                        "Validation failed",
                        fieldErrors,
                        null
                );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Triggered when someone tries to register with an email that already
     * exists.
     *
     * Returns:
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
     * Triggered when a requested resource does not exist.
     *
     * Example:
     * GET /users/999999
     *
     * Returns:
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
     * Triggered when login credentials are invalid.
     *
     * Returns:
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
     * Triggered when correct credentials are supplied but the account
     * is not ACTIVE.
     *
     * Returns:
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
     * Triggered when an ADMIN tries to create or rename a category to a
     * name that already exists.
     *
     * Returns:
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
     * Triggered when trying to delete a category that is still referenced
     * by a store or product.
     *
     * Returns:
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
     * Handles application-level authorization failures.
     *
     * Example:
     * STORE_OWNER attempts to modify another owner's resource.
     *
     * This exception is thrown explicitly by Nexora service-layer code.
     *
     * Returns:
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
     * This is different from ForbiddenOperationException.
     *
     * Spring Security throws AccessDeniedException when:
     *
     * - the user is authenticated
     * - but their role does not have permission to access the endpoint
     *
     * Example:
     *
     * CUSTOMER -> POST /api/v1/stores
     * CUSTOMER -> POST /api/v1/categories
     * STORE_OWNER -> POST /api/v1/categories
     *
     * These requests must return:
     *
     * HTTP 403 FORBIDDEN
     *
     * Previously these exceptions could reach the generic Exception handler
     * and incorrectly become HTTP 500.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(
            AccessDeniedException ex) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(
                        "Access denied. Your account does not have permission to perform this action."
                ));
    }

    /**
     * Triggered when product data passes field-level validation but is
     * logically inconsistent.
     *
     * Example:
     * discount greater than product price.
     *
     * Returns:
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
     * Triggered when trying to reduce inventory by more than the available
     * stock.
     *
     * Returns:
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
     * Triggered when trying to add or increase a product in a cart while
     * the product is unavailable.
     *
     * Returns:
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
     * Triggered when checkout is attempted with an empty cart.
     *
     * Returns:
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
     * Triggered when an order status transition is invalid.
     *
     * Example:
     * DELIVERED -> PREPARING
     *
     * Returns:
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
     * Triggered when trying to hard-delete a product that is referenced
     * by an existing order.
     *
     * Returns:
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
     * Triggered when a payment operation does not match the current order
     * or payment state.
     *
     * Example:
     * initiating payment for an already PAID order.
     *
     * Returns:
     * HTTP 409 CONFLICT
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
     * Examples:
     * - network error
     * - invalid Razorpay credentials
     * - Razorpay server-side error
     *
     * This means Nexora is functioning but an upstream payment service
     * failed.
     *
     * Returns:
     * HTTP 502 BAD_GATEWAY
     */
    @ExceptionHandler(PaymentGatewayException.class)
    public ResponseEntity<ApiResponse<Object>> handlePaymentGatewayException(
            PaymentGatewayException ex) {

        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Final safety net for unexpected exceptions.
     *
     * Returns:
     * HTTP 500 INTERNAL_SERVER_ERROR
     *
     * IMPORTANT:
     * AccessDeniedException is handled ABOVE this method, so authorization
     * failures will no longer incorrectly become HTTP 500.
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