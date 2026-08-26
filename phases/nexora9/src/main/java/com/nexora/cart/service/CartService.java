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
 * Store/Product needed an explicit "does the caller own THIS
 * resource?" check because ADMIN could act on ANY store, and a
 * STORE_OWNER could technically request another owner's store id.
 * Cart is different: every operation here starts by resolving the
 * CALLER'S OWN cart via their user id from the JWT
 * (getOrCreateCartForUser) — there is no "which cart id do you want?"
 * parameter anywhere in this API. The only place an id from the client
 * appears is a specific CART ITEM id (for update/remove), so that's
 * the one place we verify ownership: does this cart item actually
 * belong to the caller's own cart? (see assertItemBelongsToCaller).
 *
 * WHY THE STOCK CHECK HERE ISN'T A "RESERVATION":
 * addItem/updateItemQuantity validate the requested quantity against
 * Inventory's CURRENT quantity (Phase 6) purely as a friendly early
 * warning — "you're trying to add more than exists." They do NOT call
 * InventoryService.reduceStock, so nothing is actually deducted or
 * held aside. Stock isn't consumed until an order is actually placed
 * (Phase 8), using the same atomic, concurrency-safe reduceStockIfAvailable
 * this phase relies on for its check. This means it's still possible —
 * though rare — for two customers to both have the last item in their
 * carts at once; whoever checks out first gets it, and the other will
 * see an insufficient-stock error at THAT point, not before. This is
 * standard, expected e-commerce behavior (carts don't lock inventory).
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    @Transactional
    public CartResponse getCart(UserPrincipal caller) {
        Cart cart = getOrCreateCartForUser(caller);
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        return CartMapper.toResponse(cart, items);
    }

    @Transactional
    public CartResponse addItem(UserPrincipal caller, AddCartItemRequest request) {
        Cart cart = getOrCreateCartForUser(caller);
        Product product = findProductOrThrow(request.getProductId());

        if (!product.isAvailable()) {
            throw new ProductUnavailableException(product.getId());
        }

        CartItem existingItem = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), product.getId())
                .orElse(null);

        int newTotalQuantity = (existingItem != null ? existingItem.getQuantity() : 0) + request.getQuantity();
        assertQuantityDoesNotExceedStock(product.getId(), newTotalQuantity);

        if (existingItem != null) {
            // Product already in the cart — increase quantity rather
            // than create a second row (also what the unique
            // (cart_id, product_id) constraint would require anyway).
            existingItem.setQuantity(newTotalQuantity);
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(newItem);
        }

        return getCart(caller);
    }

    @Transactional
    public CartResponse updateItemQuantity(UserPrincipal caller, Long cartItemId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCartForUser(caller);
        CartItem item = findCartItemOrThrow(cartItemId);
        assertItemBelongsToCaller(item, cart);

        assertQuantityDoesNotExceedStock(item.getProduct().getId(), request.getQuantity());

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        return getCart(caller);
    }

    @Transactional
    public CartResponse removeItem(UserPrincipal caller, Long cartItemId) {
        Cart cart = getOrCreateCartForUser(caller);
        CartItem item = findCartItemOrThrow(cartItemId);
        assertItemBelongsToCaller(item, cart);

        cartItemRepository.delete(item);

        return getCart(caller);
    }

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
     * with any cart endpoint — see Cart's javadoc for why we don't
     * create one at registration time instead.
     */
    private Cart getOrCreateCartForUser(UserPrincipal caller) {
        return cartRepository.findByUserId(caller.getId())
                .orElseGet(() -> cartRepository.save(
                        Cart.builder().user(caller.getUser()).build()
                ));
    }

    private Product findProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
    }

    private CartItem findCartItemOrThrow(Long cartItemId) {
        return cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));
    }

    private void assertItemBelongsToCaller(CartItem item, Cart callerCart) {
        if (!item.getCart().getId().equals(callerCart.getId())) {
            throw new ForbiddenOperationException("This cart item does not belong to your cart.");
        }
    }

    /**
     * Friendly early check against Phase 6's Inventory — see this
     * class's javadoc for why this is a check, not a reservation.
     */
    private void assertQuantityDoesNotExceedStock(Long productId, int requestedQuantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No inventory record found for product id: " + productId));

        if (requestedQuantity > inventory.getQuantity()) {
            throw new InsufficientStockException(productId, requestedQuantity, inventory.getQuantity());
        }
    }
}
