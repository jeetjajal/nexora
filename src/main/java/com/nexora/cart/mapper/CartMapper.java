package com.nexora.cart.mapper;

import com.nexora.cart.dto.CartItemResponse;
import com.nexora.cart.dto.CartResponse;
import com.nexora.cart.entity.Cart;
import com.nexora.cart.entity.CartItem;
import com.nexora.product.entity.Product;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * THE CORE "NEVER TRUST THE FRONTEND PRICE" LOGIC LIVES HERE.
 *
 * Every price shown for a cart — unit price, line total, cart
 * subtotal — is computed FROM THE CURRENT Product ROW, read fresh out
 * of MySQL, every single time a cart is fetched. Nothing about price
 * is ever read from the CartItem row (it doesn't store one — see
 * CartItem's javadoc) or accepted from any request body. If a store
 * owner changes a product's price after it was added to someone's
 * cart, the very next time that cart is viewed, the new price is what
 * shows up — exactly as it should, since nothing has actually been
 * purchased yet.
 */
public class CartMapper {

    private CartMapper() {
    }

    public static CartResponse toResponse(Cart cart, List<CartItem> items) {
        List<CartItemResponse> itemResponses = items.stream()
                .sorted(Comparator.comparing(CartItem::getId))
                .map(CartMapper::toItemResponse)
                .collect(Collectors.toList());

        BigDecimal subtotal = itemResponses.stream()
                .map(CartItemResponse::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItemCount = itemResponses.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();

        return CartResponse.builder()
                .id(cart.getId())
                .items(itemResponses)
                .totalItemCount(totalItemCount)
                .subtotal(subtotal)
                .build();
    }

    private static CartItemResponse toItemResponse(CartItem cartItem) {
        Product product = cartItem.getProduct();

        // price - discount, read live from the Product row right now.
        BigDecimal unitPrice = product.getPrice().subtract(
                product.getDiscount() != null ? product.getDiscount() : BigDecimal.ZERO);

        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

        return CartItemResponse.builder()
                .id(cartItem.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productImageUrl(product.getImageUrl())
                .unitPrice(unitPrice)
                .quantity(cartItem.getQuantity())
                .lineTotal(lineTotal)
                .productAvailable(product.isAvailable())
                .build();
    }
}
