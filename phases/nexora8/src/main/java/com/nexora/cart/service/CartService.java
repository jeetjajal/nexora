package com.nexora.cart.service;

import com.nexora.auth.security.UserPrincipal;
import com.nexora.cart.dto.AddCartItemRequest;
import com.nexora.cart.dto.CartResponse;
import com.nexora.cart.dto.UpdateCartItemRequest;
import com.nexora.cart.entity.Cart;
import com.nexora.cart.entity.CartItem;
import com.nexora.cart.exception.ProductUnavailableException;
import com.nexora.cart.mapper.CartMapper;
import com.nexora.cart.repository.CartItemRepository;
import com.nexora.cart.repository.CartRepository;
import com.nexora.exception.ForbiddenOperationException;
import com.nexora.exception.ResourceNotFoundException;
import com.nexora.inventory.entity.Inventory;
import com.nexora.inventory.exception.InsufficientStockException;
import com.nexora.inventory.repository.InventoryRepository;
import com.nexora.product.entity.Product;
import com.nexora.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Cart service.
 *
 * A cart belongs to the currently authenticated user.
 * Cart ownership is therefore resolved using the caller's user id.
 *
 * Cart inventory checks are only early validation checks.
 * Inventory is actually reduced during checkout in Phase 8.
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    /**
     * Returns the caller's cart.
     *
     * If the user does not have a cart yet, one is created lazily.
     */
    @Transactional
    public CartResponse getCart(UserPrincipal caller) {

        Cart cart = getOrCreateCartForUser(caller);

        List<CartItem> items =
                cartItemRepository.findByCartId(cart.getId());

        return CartMapper.toResponse(cart, items);
    }

    /**
     * Adds a product to the caller's cart.
     *
     * Important:
     * We do NOT call getCart(caller) at the end because getCart()
     * internally calls getOrCreateCartForUser() again.
     *
     * When a cart is created lazily, calling getCart() here could
     * cause the repository lookup/save sequence to happen twice
     * in mocked tests.
     */
    @Transactional
    public CartResponse addItem(
            UserPrincipal caller,
            AddCartItemRequest request) {

        Cart cart = getOrCreateCartForUser(caller);

        Product product =
                findProductOrThrow(request.getProductId());

        if (!product.isAvailable()) {
            throw new ProductUnavailableException(product.getId());
        }

        CartItem existingItem =
                cartItemRepository
                        .findByCartIdAndProductId(
                                cart.getId(),
                                product.getId())
                        .orElse(null);

        int currentQuantity =
                existingItem != null
                        ? existingItem.getQuantity()
                        : 0;

        int newTotalQuantity =
                currentQuantity + request.getQuantity();

        /*
         * Check inventory before modifying the cart.
         */
        assertQuantityDoesNotExceedStock(
                product.getId(),
                newTotalQuantity
        );

        if (existingItem != null) {

            /*
             * Product already exists in the cart.
             * Increase its quantity instead of creating
             * another CartItem row.
             */
            existingItem.setQuantity(newTotalQuantity);

            cartItemRepository.save(existingItem);

        } else {

            /*
             * Product does not exist in the cart.
             * Create the first CartItem row.
             */
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();

            cartItemRepository.save(newItem);
        }

        /*
         * Build the response directly from the cart we already
         * resolved above.
         *
         * DO NOT call getCart(caller) here.
         */
        List<CartItem> items =
                cartItemRepository.findByCartId(cart.getId());

        return CartMapper.toResponse(cart, items);
    }

    /**
     * Updates a cart item's quantity.
     */
    @Transactional
    public CartResponse updateItemQuantity(
            UserPrincipal caller,
            Long cartItemId,
            UpdateCartItemRequest request) {

        Cart cart = getOrCreateCartForUser(caller);

        CartItem item =
                findCartItemOrThrow(cartItemId);

        assertItemBelongsToCaller(item, cart);

        assertQuantityDoesNotExceedStock(
                item.getProduct().getId(),
                request.getQuantity()
        );

        item.setQuantity(request.getQuantity());

        cartItemRepository.save(item);

        return getCart(caller);
    }

    /**
     * Removes a cart item.
     */
    @Transactional
    public CartResponse removeItem(
            UserPrincipal caller,
            Long cartItemId) {

        Cart cart = getOrCreateCartForUser(caller);

        CartItem item =
                findCartItemOrThrow(cartItemId);

        assertItemBelongsToCaller(item, cart);

        cartItemRepository.delete(item);

        return getCart(caller);
    }

    /**
     * Removes all items from the caller's cart.
     */
    @Transactional
    public CartResponse clearCart(
            UserPrincipal caller) {

        Cart cart = getOrCreateCartForUser(caller);

        cartItemRepository.deleteByCartId(cart.getId());

        return getCart(caller);
    }

    // ============================================================
    // Internal helpers
    // ============================================================

    /**
     * Finds the caller's cart.
     *
     * If no cart exists, creates it lazily.
     */
    private Cart getOrCreateCartForUser(
            UserPrincipal caller) {

        return cartRepository
                .findByUserId(caller.getId())
                .orElseGet(() ->
                        cartRepository.save(
                                Cart.builder()
                                        .user(caller.getUser())
                                        .build()
                        )
                );
    }

    /**
     * Finds a product or throws 404.
     */
    private Product findProductOrThrow(Long productId) {

        return productRepository
                .findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with id: "
                                        + productId
                        )
                );
    }

    /**
     * Finds a cart item or throws 404.
     */
    private CartItem findCartItemOrThrow(
            Long cartItemId) {

        return cartItemRepository
                .findById(cartItemId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cart item not found with id: "
                                        + cartItemId
                        )
                );
    }

    /**
     * Ensures the cart item belongs to the caller's cart.
     */
    private void assertItemBelongsToCaller(
            CartItem item,
            Cart callerCart) {

        if (!item.getCart()
                .getId()
                .equals(callerCart.getId())) {

            throw new ForbiddenOperationException(
                    "This cart item does not belong to your cart."
            );
        }
    }

    /**
     * Performs an early inventory check.
     *
     * This does not reserve or deduct inventory.
     * Actual inventory deduction happens during checkout.
     */
    private void assertQuantityDoesNotExceedStock(
            Long productId,
            int requestedQuantity) {

        Inventory inventory =
                inventoryRepository
                        .findByProductId(productId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "No inventory record found for product id: "
                                                + productId
                                )
                        );

        if (requestedQuantity > inventory.getQuantity()) {

            throw new InsufficientStockException(
                    productId,
                    requestedQuantity,
                    inventory.getQuantity()
            );
        }
    }
}