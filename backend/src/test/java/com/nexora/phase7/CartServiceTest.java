package com.nexora.phase7;

import com.nexora.auth.security.UserPrincipal;
import com.nexora.cart.dto.AddCartItemRequest;
import com.nexora.cart.dto.CartResponse;
import com.nexora.cart.dto.UpdateCartItemRequest;
import com.nexora.cart.entity.Cart;
import com.nexora.cart.entity.CartItem;
import com.nexora.cart.exception.ProductUnavailableException;
import com.nexora.cart.repository.CartItemRepository;
import com.nexora.cart.repository.CartRepository;
import com.nexora.cart.service.CartService;
import com.nexora.exception.ForbiddenOperationException;
import com.nexora.exception.ResourceNotFoundException;
import com.nexora.inventory.entity.Inventory;
import com.nexora.inventory.exception.InsufficientStockException;
import com.nexora.inventory.repository.InventoryRepository;
import com.nexora.product.entity.Product;
import com.nexora.product.repository.ProductRepository;
import com.nexora.role.entity.Role;
import com.nexora.role.entity.RoleName;
import com.nexora.store.entity.Store;
import com.nexora.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartService unit tests (Phase 7)")
class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private InventoryRepository inventoryRepository;

    private CartService cartService;

    private User customer;
    private UserPrincipal principal;
    private Cart cart;
    private Product product;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartRepository, cartItemRepository, productRepository, inventoryRepository);

        Role customerRole = Role.builder().id(1L).name(RoleName.CUSTOMER).build();
        customer = User.builder().id(5L).name("Customer").email("customer@example.com")
                .password("hashed").status("ACTIVE").roles(Set.of(customerRole)).build();
        principal = new UserPrincipal(customer);

        cart = Cart.builder().id(50L).user(customer).build();

        Store store = Store.builder().id(1L).name("Store").build();
        product = Product.builder().id(100L).store(store).name("Pizza")
                .price(new BigDecimal("200.00")).discount(new BigDecimal("20.00"))
                .available(true).build();
    }

    private Inventory inventoryWith(int quantity) {
        return Inventory.builder().id(1L).product(product).quantity(quantity).lowStockThreshold(5).build();
    }

    // ==============================================================
    // Add item
    // ==============================================================

    @Test
    @DisplayName("Adding a new product creates a new cart item")
    void addingNewProductCreatesNewItem() {
        when(cartRepository.findByUserId(5L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(50L, 100L)).thenReturn(Optional.empty());
        when(inventoryRepository.findByProductId(100L)).thenReturn(Optional.of(inventoryWith(10)));
        when(cartItemRepository.findByCartId(50L)).thenReturn(List.of(
                CartItem.builder().id(1L).cart(cart).product(product).quantity(2).build()));

        CartResponse response = cartService.addItem(principal, new AddCartItemRequest(100L, 2));

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(2);
        // unitPrice = 200.00 - 20.00 = 180.00 ; lineTotal = 180 * 2 = 360.00
        assertThat(response.getItems().get(0).getUnitPrice()).isEqualByComparingTo("180.00");
        assertThat(response.getSubtotal()).isEqualByComparingTo("360.00");
        verify(cartItemRepository).save(argThat(item -> item.getQuantity() == 2));
    }

    @Test
    @DisplayName("Adding a product already in the cart increases its existing quantity")
    void addingExistingProductIncreasesQuantity() {
        CartItem existing = CartItem.builder().id(1L).cart(cart).product(product).quantity(3).build();

        when(cartRepository.findByUserId(5L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(50L, 100L)).thenReturn(Optional.of(existing));
        when(inventoryRepository.findByProductId(100L)).thenReturn(Optional.of(inventoryWith(20)));
        when(cartItemRepository.findByCartId(50L)).thenReturn(List.of(existing));

        cartService.addItem(principal, new AddCartItemRequest(100L, 2));

        // existing quantity (3) + newly requested (2) = 5
        verify(cartItemRepository).save(argThat(item -> item.getQuantity() == 5));
        verify(cartItemRepository, never()).save(argThat(item -> item != existing));
    }

    @Test
    @DisplayName("Adding an unavailable product throws ProductUnavailableException")
    void addingUnavailableProductThrows() {
        product.setAvailable(false);

        when(cartRepository.findByUserId(5L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addItem(principal, new AddCartItemRequest(100L, 1)))
                .isInstanceOf(ProductUnavailableException.class);

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Adding more than available stock throws InsufficientStockException")
    void addingMoreThanStockThrows() {
        when(cartRepository.findByUserId(5L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(50L, 100L)).thenReturn(Optional.empty());
        when(inventoryRepository.findByProductId(100L)).thenReturn(Optional.of(inventoryWith(3)));

        assertThatThrownBy(() -> cartService.addItem(principal, new AddCartItemRequest(100L, 5)))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("requested 5")
                .hasMessageContaining("only 3 available");

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Adding a nonexistent product throws ResourceNotFoundException")
    void addingUnknownProductThrows404() {
        when(cartRepository.findByUserId(5L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(principal, new AddCartItemRequest(999L, 1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("A cart is created automatically the first time a user adds an item")
    void cartIsCreatedLazily() {
        when(cartRepository.findByUserId(5L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(50L, 100L)).thenReturn(Optional.empty());
        when(inventoryRepository.findByProductId(100L)).thenReturn(Optional.of(inventoryWith(10)));
        when(cartItemRepository.findByCartId(50L)).thenReturn(List.of());

        cartService.addItem(principal, new AddCartItemRequest(100L, 1));

        verify(cartRepository).save(argThat(c -> c.getUser().getId().equals(5L)));
    }

    // ==============================================================
    // Update item quantity
    // ==============================================================

    @Test
    @DisplayName("Updating a cart item's quantity to an absolute value succeeds")
    void updateItemQuantitySucceeds() {
        CartItem item = CartItem.builder().id(1L).cart(cart).product(product).quantity(2).build();

        when(cartRepository.findByUserId(5L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(inventoryRepository.findByProductId(100L)).thenReturn(Optional.of(inventoryWith(10)));
        when(cartItemRepository.findByCartId(50L)).thenReturn(List.of(item));

        cartService.updateItemQuantity(principal, 1L, new UpdateCartItemRequest(7));

        verify(cartItemRepository).save(argThat(i -> i.getQuantity() == 7));
    }

    @Test
    @DisplayName("Cannot update a cart item that belongs to another user's cart")
    void cannotUpdateAnotherUsersCartItem() {
        Cart someoneElsesCart = Cart.builder().id(999L).build();
        CartItem someoneElsesItem = CartItem.builder().id(1L).cart(someoneElsesCart).product(product).quantity(2).build();

        when(cartRepository.findByUserId(5L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(someoneElsesItem));

        assertThatThrownBy(() -> cartService.updateItemQuantity(principal, 1L, new UpdateCartItemRequest(7)))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(cartItemRepository, never()).save(any());
    }

    // ==============================================================
    // Remove item / clear cart
    // ==============================================================

    @Test
    @DisplayName("Removing a cart item deletes it")
    void removeItemDeletesIt() {
        CartItem item = CartItem.builder().id(1L).cart(cart).product(product).quantity(2).build();

        when(cartRepository.findByUserId(5L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(cartItemRepository.findByCartId(50L)).thenReturn(List.of());

        CartResponse response = cartService.removeItem(principal, 1L);

        verify(cartItemRepository).delete(item);
        assertThat(response.getItems()).isEmpty();
    }

    @Test
    @DisplayName("Cannot remove a cart item that belongs to another user's cart")
    void cannotRemoveAnotherUsersCartItem() {
        Cart someoneElsesCart = Cart.builder().id(999L).build();
        CartItem someoneElsesItem = CartItem.builder().id(1L).cart(someoneElsesCart).product(product).quantity(2).build();

        when(cartRepository.findByUserId(5L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(someoneElsesItem));

        assertThatThrownBy(() -> cartService.removeItem(principal, 1L))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(cartItemRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Clearing the cart removes all items")
    void clearCartRemovesAllItems() {
        when(cartRepository.findByUserId(5L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(50L)).thenReturn(List.of());

        CartResponse response = cartService.clearCart(principal);

        verify(cartItemRepository).deleteByCartId(50L);
        assertThat(response.getItems()).isEmpty();
        assertThat(response.getSubtotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ==============================================================
    // Price integrity — never trust a frontend price
    // ==============================================================

    @Test
    @DisplayName("Cart totals always reflect the product's CURRENT price, never a cached/stale one")
    void cartReflectsCurrentProductPrice() {
        CartItem item = CartItem.builder().id(1L).cart(cart).product(product).quantity(1).build();

        when(cartRepository.findByUserId(5L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(50L)).thenReturn(List.of(item));

        CartResponse before = cartService.getCart(principal);
        assertThat(before.getItems().get(0).getUnitPrice()).isEqualByComparingTo("180.00"); // 200 - 20

        // Simulate the store owner changing the price after it was added to the cart
        product.setPrice(new BigDecimal("150.00"));
        product.setDiscount(BigDecimal.ZERO);

        CartResponse after = cartService.getCart(principal);
        assertThat(after.getItems().get(0).getUnitPrice()).isEqualByComparingTo("150.00");
    }
}
