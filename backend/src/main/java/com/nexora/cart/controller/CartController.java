package com.nexora.cart.controller;

import com.nexora.auth.security.UserPrincipal;
import com.nexora.cart.dto.AddCartItemRequest;
import com.nexora.cart.dto.CartResponse;
import com.nexora.cart.dto.UpdateCartItemRequest;
import com.nexora.cart.service.CartService;
import com.nexora.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Every endpoint here requires a valid JWT (SecurityConfig's
 * "anyRequest().authenticated()" from Phase 3 — unchanged). No
 * @PreAuthorize role restriction: any authenticated user (CUSTOMER,
 * STORE_OWNER, DELIVERY_PARTNER, or ADMIN) has exactly one cart of
 * their own — there's no reason to prevent, say, a STORE_OWNER from
 * also shopping on the platform as a buyer.
 *
 * There is deliberately no {cartId} anywhere in these paths — the
 * cart operated on is always "whichever cart belongs to the caller,"
 * resolved server-side from the JWT, never supplied by the client.
 */
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@AuthenticationPrincipal UserPrincipal principal) {
        CartResponse response = cartService.getCart(principal);
        return ResponseEntity.ok(ApiResponse.success("Cart fetched successfully", response));
    }

    /**
     * Adding a product already in the cart increases its quantity
     * rather than creating a duplicate line.
     *
     * Sample request: { "productId": 42, "quantity": 2 }
     */
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @Valid @RequestBody AddCartItemRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        CartResponse response = cartService.addItem(principal, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Item added to cart successfully", response));
    }

    /**
     * Sets a cart item's quantity to an absolute value.
     *
     * Sample request: { "quantity": 5 }
     */
    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateItemQuantity(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        CartResponse response = cartService.updateItemQuantity(principal, itemId, request);
        return ResponseEntity.ok(ApiResponse.success("Cart item updated successfully", response));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserPrincipal principal) {

        CartResponse response = cartService.removeItem(principal, itemId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart successfully", response));
    }

    /**
     * Removes every item from the caller's cart in one call.
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<CartResponse>> clearCart(@AuthenticationPrincipal UserPrincipal principal) {
        CartResponse response = cartService.clearCart(principal);
        return ResponseEntity.ok(ApiResponse.success("Cart cleared successfully", response));
    }
}
