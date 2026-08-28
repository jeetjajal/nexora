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
 * CART BUSINESS LOGIC.
 *
 * Each cart belongs to exactly one user.
 *
 * The API never accepts a cart id for normal cart operations. Instead,
 * the authenticated user's id from the JWT is used to locate the
 * caller's own cart.
 *
 * This means a CUSTOMER cannot simply change a cart id in the request
 * and access another customer's cart.
 *
 * CART CREATION:
 *
 * Carts are created lazily. A cart is created only when the customer
 * first interacts with the cart.
 *
 * Stock is checked here only as an early availability check.
 * Inventory is NOT reserved by the cart.
 *
 * Actual stock deduction happens during checkout in the order flow.
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    /**
     * Gets the current user's cart.
     *
     * If the user does not have a cart yet, one is created.
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
     * If no cart exists, it is created exactly once during this
     * operation.
     *
     * After modifying the cart, we directly load its items instead
     * of calling getCart(), because getCart() would perform another
     * get-or-create lookup.
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
                                product.getId()
                        )
                        .orElse(null);

        int newTotalQuantity =
                (existingItem != null
                        ? existingItem.getQuantity()
                        : 0)
                        + request.getQuantity();

        /*
         * This is an availability check only.
         * It does not reserve or deduct stock.
         */
        assertQuantityDoesNotExceedStock(
                product.getId(),
                newTotalQuantity
        );

        if (existingItem != null) {

            /*
             * Product already exists in the cart.
             * Increase its quantity instead of creating another row.
             */
            existingItem.setQuantity(newTotalQuantity);
            cartItemRepository.save(existingItem);

        } else {

            CartItem newItem =
                    CartItem.builder()
                            .cart(cart)
                            .product(product)
                            .quantity(request.getQuantity())
                            .build();

            cartItemRepository.save(newItem);
        }

        /*
         * Do NOT call getCart(caller) here.
         *
         * getCart() would call getOrCreateCartForUser() again.
         * In real JPA usage that is unnecessary work, and in unit tests
         * where findByUserId() is intentionally stubbed to return empty,
         * it can appear as if the cart is being created twice.
         *
         * We already have the Cart object, so simply load its items.
         */
        List<CartItem> items =
                cartItemRepository.findByCartId(cart.getId());

        return CartMapper.toResponse(cart, items);
    }

    /**
     * Updates the quantity of an existing cart item.
     *
     * The cart item must belong to the caller's cart.
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
     * Removes one cart item.
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

    // ------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------

    /**
     * Lazily creates a Cart for the authenticated user.
     *
     * The caller's user id comes from the authenticated JWT rather
     * than from request data.
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
     * Finds a product or throws a 404-style application exception.
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
     * Finds a cart item or throws a 404-style application exception.
     */
    private CartItem findCartItemOrThrow(Long cartItemId) {

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
     * Prevents a customer from manipulating a cart item belonging
     * to another customer's cart.
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
     * Friendly early stock check.
     *
     * This does NOT reserve stock.
     *
     * Actual atomic stock reduction happens during checkout.
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