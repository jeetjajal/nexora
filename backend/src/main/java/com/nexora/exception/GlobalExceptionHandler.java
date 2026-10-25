package com.nexora.exception;

import com.nexora.auth.exception.AccountNotActiveException;
import com.nexora.auth.exception.InvalidCredentialsException;
import com.nexora.cart.exception.ProductUnavailableException;
import com.nexora.category.exception.CategoryInUseException;
import com.nexora.category.exception.DuplicateCategoryException;
import com.nexora.common.ApiResponse;
import com.nexora.idempotency.exception.IdempotencyConflictException;
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
 * WHAT IS GLOBAL EXCEPTION HANDLING?
 *
 * Instead of writing try/catch in every controller method,
 * @RestControllerAdvice provides one central place for converting
 * exceptions into clean and consistent HTTP responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Triggered when @Valid validation fails.
     *
     * Example:
     * - missing required field
     * - invalid email
     * - invalid request data
     *
     * Returns 400 BAD_REQUEST.
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
     * Duplicate email registration.
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
     * Requested entity/resource does not exist.
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
     * Invalid username/email/password during login.
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
     * User is authenticated but account is not active.
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
     * Duplicate category.
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
     * Category cannot be deleted because it is still being used.
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
     * Nexora business-level authorization failure.
     *
     * Example:
     * STORE_OWNER tries to modify another owner's store.
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
     * Spring Security method-level authorization failure.
     *
     * This is important for @PreAuthorize.
     *
     * Example:
     *
     * @PreAuthorize("hasRole('ADMIN')")
     *
     * If CUSTOMER calls that endpoint, Spring Security throws
     * AccessDeniedException before the controller/service operation
     * is allowed to execute.
     *
     * Without this handler the exception can reach the generic
     * Exception handler and incorrectly become HTTP 500.
     *
     * Correct response: 403 FORBIDDEN.
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
     * Product data is logically invalid.
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
     * Requested stock quantity exceeds available inventory.
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
     * Product is unavailable for cart operation.
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
     * Checkout attempted with an empty cart.
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
     * Invalid order state-machine transition.
     *
     * Example:
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
     * Product cannot be deleted because an order references it.
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
     * Payment operation is incompatible with the current payment/order state.
     *
     * Example:
     * trying to initiate another payment for an already paid order.
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
     * Razorpay/gateway communication failure.
     *
     * Examples:
     * - network problem
     * - invalid gateway credentials
     * - Razorpay unavailable
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
     * Razorpay callback/webhook signature verification failed.
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
     * Phase 11 idempotency conflict.
     *
     * Happens when:
     *
     * 1. The same Idempotency-Key is currently IN_PROGRESS.
     *
     * OR
     *
     * 2. The same key is reused for a different request fingerprint.
     *
     * Returns 409 CONFLICT.
     */
    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ApiResponse<Object>> handleIdempotencyConflict(
            IdempotencyConflictException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Final safety net.
     *
     * Any exception that does not have a specific handler reaches here.
     *
     * Returns 500 INTERNAL_SERVER_ERROR.
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