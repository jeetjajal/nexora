package com.nexora.cart.repository;

import com.nexora.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCartId(Long cartId);

    /**
     * Used by CartService.addItem to check "is this product already
     * in the cart?" — if so, we increase its quantity instead of
     * inserting a second row for the same product (the unique
     * constraint on cart_items would reject a duplicate anyway, but
     * checking first lets us give a proper "increased quantity"
     * response instead of a raw constraint-violation error).
     */
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    void deleteByCartId(Long cartId);
}
