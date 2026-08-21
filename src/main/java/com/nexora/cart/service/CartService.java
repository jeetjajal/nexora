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
 * WHY NO OWNERSHIP-CHECK PATTERN LIKE StoreService/ProductService?
 *
 * Store/Product needed an explicit ownership check because ADMIN can
 * act on any store/product and STORE_OWNER can request another owner's
 * resource id.
 *
 * Cart is different. Every operation starts by resolving the CALLER'S
 * OWN cart using the user id from the JWT. There is no cart id supplied
 * by the client.
 *
 * For update/remove operations, the client supplies a CartItem id.
 * Therefore we verify that the CartItem belongs to the caller's cart.
 *
 * WHY STOCK IS CHECKED HERE BUT NOT RESERVED:
 *
 * addItem/updateItemQuantity only perform an early validation against
 * the current inventory quantity. They do not reduce or reserve stock.
 *
 * Actual stock reduction will happen during checkout/order processing.
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    /**
     * Get the caller's cart.
     *
     * If the user has never used the cart before, the cart is created
     * lazily.
     */
    @Transactional
    public CartResponse getCart(UserPrincipal caller) {
        Cart cart = getOrCreateCartForUser(caller);

        List<CartItem> items =
                cartItemRepository.findByCartId(cart.getId());

        return CartMapper.toResponse(cart, items);
    }

    /**
     * Add a product to the caller's cart.
     *
     * Important:
     * The Cart object created here is reused when constructing the
     * response. We deliberately do NOT call getCart(caller) at the end,
     * because that would perform another cart lookup and could attempt
     * to create a second cart when the cart was just created.
     */
    @Transactional
    public CartResponse addItem(
            UserPrincipal caller,
            AddCartItemRequest request) {

        // Resolve/create the caller's cart exactly once.
        Cart cart = getOrCreateCartForUser(caller);

        Product product = findProductOrThrow(request.getProductId());

        if (!product.isAvailable()) {
            throw new ProductUnavailableException(product.getId());
        }

        CartItem existingItem =
                cartItemRepository
                        .findByCartIdAndProductId(
                                cart.getId(),
                                product.getId())
                        .orElse(null);

        int newTotalQuantity =
                (existingItem != null
                        ? existingItem.getQuantity()
                        : 0)
                        + request.getQuantity();

        assertQuantityDoesNotExceedStock(
                product.getId(),
                newTotalQuantity);

        if (existingItem != null) {

            // Product already exists in cart.
            existingItem.setQuantity(newTotalQuantity);

            cartItemRepository.save(existingItem);

        } else {

            // Product does not exist in cart.
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();

            cartItemRepository.save(newItem);
        }

        /*
         * IMPORTANT:
         *
         * Do not call getCart(caller) here.
         *
         * getCart() calls getOrCreateCartForUser() again.
         * During Mockito testing, the repository may still return
         * Optional.empty(), causing a second cartRepository.save().
         *
         * We already have the correct Cart instance, so simply load
         * its items and create the response.
         */
        List<CartItem> items =
                cartItemRepository.findByCartId(cart.getId());

        return CartMapper.toResponse(cart, items);
    }

    /**
     * Update an existing cart item's absolute quantity.
     */
    @Transactional
    public CartResponse updateItemQuantity(
            UserPrincipal caller,
            Long cartItemId,
            UpdateCartItemRequest request) {

        Cart cart = getOrCreateCartForUser(caller);

        CartItem item = findCartItemOrThrow(cartItemId);

        assertItemBelongsToCaller(item, cart);

        assertQuantityDoesNotExceedStock(
                item.getProduct().getId(),
                request.getQuantity());

        item.setQuantity(request.getQuantity());

        cartItemRepository.save(item);

        return getCart(caller);
    }

    /**
     * Remove a cart item.
     */
    @Transactional
    public CartResponse removeItem(
            UserPrincipal caller,
            Long cartItemId) {

        Cart cart = getOrCreateCartForUser(caller);

        CartItem item = findCartItemOrThrow(cartItemId);

        assertItemBelongsToCaller(item, cart);

        cartItemRepository.delete(item);

        return getCart(caller);
    }

    /**
     * Remove all items from the caller's cart.
     */
    @Transactional
    public CartResponse clearCart(UserPrincipal caller) {

        Cart cart = getOrCreateCartForUser(caller);

        cartItemRepository.deleteByCartId(cart.getId());

        return getCart(caller);
    }

    // ------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------

    /**
     * Lazily creates a Cart for a user the first time they interact
     * with any cart endpoint.
     *
     * This method performs only one lookup and one save when the cart
     * does not already exist.
     */
    private Cart getOrCreateCartForUser(UserPrincipal caller) {

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
     * Find a product or throw 404.
     */
    private Product findProductOrThrow(Long productId) {

        return productRepository
                .findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with id: "
                                        + productId));
    }

    /**
     * Find a cart item or throw 404.
     */
    private CartItem findCartItemOrThrow(Long cartItemId) {

        return cartItemRepository
                .findById(cartItemId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cart item not found with id: "
                                        + cartItemId));
    }

    /**
     * Verify that the cart item belongs to the caller's cart.
     */
    private void assertItemBelongsToCaller(
            CartItem item,
            Cart callerCart) {

        if (!item.getCart()
                .getId()
                .equals(callerCart.getId())) {

            throw new ForbiddenOperationException(
                    "This cart item does not belong to your cart.");
        }
    }

    /**
     * Friendly early stock check.
     *
     * This does not reserve or reduce inventory.
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
                                                + productId));

        if (requestedQuantity > inventory.getQuantity()) {

            throw new InsufficientStockException(
                    productId,
                    requestedQuantity,
                    inventory.getQuantity());
        }
    }
}