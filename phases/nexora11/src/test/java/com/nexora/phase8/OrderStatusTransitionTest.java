package com.nexora.phase8;

import com.nexora.address.entity.Address;
import com.nexora.address.service.AddressService;
import com.nexora.auth.security.UserPrincipal;
import com.nexora.cart.repository.CartItemRepository;
import com.nexora.cart.repository.CartRepository;
import com.nexora.exception.ForbiddenOperationException;
import com.nexora.inventory.service.InventoryService;
import com.nexora.order.dto.OrderResponse;
import com.nexora.order.dto.OrderStatusUpdateRequest;
import com.nexora.order.entity.Order;
import com.nexora.order.entity.OrderItem;
import com.nexora.order.entity.OrderStatus;
import com.nexora.order.exception.InvalidOrderStatusTransitionException;
import com.nexora.order.repository.OrderItemRepository;
import com.nexora.order.repository.OrderRepository;
import com.nexora.order.service.OrderService;
import com.nexora.product.entity.Product;
import com.nexora.role.entity.Role;
import com.nexora.role.entity.RoleName;
import com.nexora.store.entity.Store;
import com.nexora.store.repository.StoreRepository;
import com.nexora.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService state machine unit tests (Phase 8)")
class OrderStatusTransitionTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private AddressService addressService;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private com.nexora.idempotency.service.IdempotencyService idempotencyService;

    private OrderService orderService;

    private User ownerUser;
    private User otherOwnerUser;
    private User adminUser;
    private User customerUser;
    private Store store;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository,
                orderItemRepository,
                cartRepository,
                cartItemRepository,
                storeRepository,
                addressService,
                inventoryService,
                idempotencyService
        );

        ReflectionTestUtils.setField(
                orderService,
                "deliveryFee",
                new BigDecimal("40.00")
        );

        Role storeOwnerRole = Role.builder()
                .id(1L)
                .name(RoleName.STORE_OWNER)
                .build();

        Role adminRole = Role.builder()
                .id(2L)
                .name(RoleName.ADMIN)
                .build();

        Role customerRole = Role.builder()
                .id(3L)
                .name(RoleName.CUSTOMER)
                .build();

        ownerUser = User.builder()
                .id(10L)
                .name("Owner")
                .email("owner@example.com")
                .password("hashed")
                .status("ACTIVE")
                .roles(Set.of(storeOwnerRole))
                .build();

        otherOwnerUser = User.builder()
                .id(11L)
                .name("Other Owner")
                .email("other@example.com")
                .password("hashed")
                .status("ACTIVE")
                .roles(Set.of(storeOwnerRole))
                .build();

        adminUser = User.builder()
                .id(99L)
                .name("Admin")
                .email("admin@example.com")
                .password("hashed")
                .status("ACTIVE")
                .roles(Set.of(adminRole))
                .build();

        customerUser = User.builder()
                .id(20L)
                .name("Customer")
                .email("customer@example.com")
                .password("hashed")
                .status("ACTIVE")
                .roles(Set.of(customerRole))
                .build();

        store = Store.builder()
                .id(100L)
                .owner(ownerUser)
                .name("Test Store")
                .build();
    }

    private Order orderWithStatus(OrderStatus status) {

        /*
         * OrderMapper requires a delivery address when converting
         * Order -> OrderResponse.
         *
         * The original fixture did not provide one, which caused
         * NullPointerException in tests that successfully reached
         * response mapping.
         */
        Address deliveryAddress = Address.builder()
                .id(300L)
                .user(customerUser)
                .addressLine1("123 Test Street")
                .city("Rajkot")
                .state("Gujarat")
                .country("India")
                .pincode("360001")
                .isDefault(false)
                .build();

        Product product = Product.builder()
                .id(500L)
                .store(store)
                .name("Test Product")
                .price(new BigDecimal("50.00"))
                .build();

        Order order = Order.builder()
                .id(1L)
                .user(customerUser)
                .store(store)
                .deliveryAddress(deliveryAddress)
                .status(status)
                .subtotal(new BigDecimal("100.00"))
                .deliveryFee(new BigDecimal("40.00"))
                .totalAmount(new BigDecimal("140.00"))
                .build();

        OrderItem item = OrderItem.builder()
                .id(1L)
                .order(order)
                .product(product)
                .productNameSnapshot("Test Product")
                .unitPrice(new BigDecimal("50.00"))
                .quantity(2)
                .subtotal(new BigDecimal("100.00"))
                .build();

        order.setItems(Set.of(item));

        return order;
    }

    // ==============================================================
    // Valid transitions succeed
    // ==============================================================

    @Test
    @DisplayName("CREATED -> PAYMENT_PENDING is allowed")
    void createdToPaymentPendingAllowed() {
        Order order = orderWithStatus(OrderStatus.CREATED);

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.updateStatus(
                new UserPrincipal(ownerUser),
                1L,
                new OrderStatusUpdateRequest(
                        OrderStatus.PAYMENT_PENDING
                )
        );

        assertThat(response.getStatus())
                .isEqualTo("PAYMENT_PENDING");
    }

    @Test
    @DisplayName("The full happy-path chain CREATED -> ... -> DELIVERED is all individually valid")
    void fullHappyPathChainIsValid() {

        OrderStatus[] chain = {
                OrderStatus.CREATED,
                OrderStatus.PAYMENT_PENDING,
                OrderStatus.PAID,
                OrderStatus.CONFIRMED,
                OrderStatus.PREPARING,
                OrderStatus.READY,
                OrderStatus.RIDER_ASSIGNED,
                OrderStatus.PICKED_UP,
                OrderStatus.OUT_FOR_DELIVERY,
                OrderStatus.DELIVERED
        };

        for (int i = 0; i < chain.length - 1; i++) {

            Order order = orderWithStatus(chain[i]);

            when(orderRepository.findById(1L))
                    .thenReturn(Optional.of(order));

            when(orderRepository.save(any(Order.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            OrderResponse response = orderService.updateStatus(
                    new UserPrincipal(ownerUser),
                    1L,
                    new OrderStatusUpdateRequest(chain[i + 1])
            );

            assertThat(response.getStatus())
                    .isEqualTo(chain[i + 1].name());
        }
    }

    // ==============================================================
    // Invalid transitions rejected
    // ==============================================================

    @Test
    @DisplayName("DELIVERED -> PREPARING is rejected (the exact example from the roadmap)")
    void deliveredToPreparingRejected() {

        Order order = orderWithStatus(OrderStatus.DELIVERED);

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() ->
                orderService.updateStatus(
                        new UserPrincipal(ownerUser),
                        1L,
                        new OrderStatusUpdateRequest(
                                OrderStatus.PREPARING
                        )
                )
        )
                .isInstanceOf(
                        InvalidOrderStatusTransitionException.class
                )
                .hasMessageContaining("DELIVERED")
                .hasMessageContaining("PREPARING");

        verify(orderRepository, never())
                .save(any());
    }

    @ParameterizedTest
    @EnumSource(
            value = OrderStatus.class,
            names = {"DELIVERED", "CANCELLED", "REJECTED"}
    )
    @DisplayName("Terminal statuses (DELIVERED, CANCELLED, REJECTED) allow no further transitions at all")
    void terminalStatusesAllowNoTransitions(
            OrderStatus terminalStatus
    ) {

        Order order = orderWithStatus(terminalStatus);

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() ->
                orderService.updateStatus(
                        new UserPrincipal(ownerUser),
                        1L,
                        new OrderStatusUpdateRequest(
                                OrderStatus.CONFIRMED
                        )
                )
        )
                .isInstanceOf(
                        InvalidOrderStatusTransitionException.class
                );
    }

    @Test
    @DisplayName("Skipping states (CREATED -> DELIVERED) is rejected")
    void skippingStatesRejected() {

        Order order = orderWithStatus(OrderStatus.CREATED);

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() ->
                orderService.updateStatus(
                        new UserPrincipal(ownerUser),
                        1L,
                        new OrderStatusUpdateRequest(
                                OrderStatus.DELIVERED
                        )
                )
        )
                .isInstanceOf(
                        InvalidOrderStatusTransitionException.class
                );
    }

    // ==============================================================
    // Authorization
    // ==============================================================

    @Test
    @DisplayName("A different STORE_OWNER cannot update another store's order status")
    void otherOwnerCannotUpdateStatus() {

        Order order = orderWithStatus(OrderStatus.CREATED);

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() ->
                orderService.updateStatus(
                        new UserPrincipal(otherOwnerUser),
                        1L,
                        new OrderStatusUpdateRequest(
                                OrderStatus.PAYMENT_PENDING
                        )
                )
        )
                .isInstanceOf(
                        ForbiddenOperationException.class
                );

        verify(orderRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("ADMIN can update any store's order status")
    void adminCanUpdateAnyOrderStatus() {

        Order order = orderWithStatus(OrderStatus.CREATED);

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.updateStatus(
                new UserPrincipal(adminUser),
                1L,
                new OrderStatusUpdateRequest(
                        OrderStatus.PAYMENT_PENDING
                )
        );

        assertThat(response.getStatus())
                .isEqualTo("PAYMENT_PENDING");
    }

    // ==============================================================
    // Customer self-cancel
    // ==============================================================

    @Test
    @DisplayName("Customer can cancel their own order while still CREATED")
    void customerCanCancelWhileCreated() {

        Order order = orderWithStatus(OrderStatus.CREATED);

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.cancelOwnOrder(
                new UserPrincipal(customerUser),
                1L
        );

        assertThat(response.getStatus())
                .isEqualTo("CANCELLED");

        verify(inventoryService)
                .restoreStockForOrder(any(), anyInt());
    }

    @Test
    @DisplayName("Customer CANNOT cancel their own order once it's PREPARING")
    void customerCannotCancelOncePreparing() {

        Order order = orderWithStatus(OrderStatus.PREPARING);

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() ->
                orderService.cancelOwnOrder(
                        new UserPrincipal(customerUser),
                        1L
                )
        )
                .isInstanceOf(
                        InvalidOrderStatusTransitionException.class
                );

        verify(orderRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("A different customer cannot cancel someone else's order")
    void differentCustomerCannotCancel() {

        Order order = orderWithStatus(OrderStatus.CREATED);

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        User anotherCustomer = User.builder()
                .id(21L)
                .name("Another Customer")
                .email("another@example.com")
                .password("hashed")
                .status("ACTIVE")
                .roles(Set.of(
                        Role.builder()
                                .id(3L)
                                .name(RoleName.CUSTOMER)
                                .build()
                ))
                .build();

        assertThatThrownBy(() ->
                orderService.cancelOwnOrder(
                        new UserPrincipal(anotherCustomer),
                        1L
                )
        )
                .isInstanceOf(
                        ForbiddenOperationException.class
                );
    }

    @Test
    @DisplayName("Cancelling an order via the status endpoint restores stock")
    void cancellingViaStatusEndpointRestoresStock() {

        Order order = orderWithStatus(OrderStatus.CREATED);

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        orderService.updateStatus(
                new UserPrincipal(ownerUser),
                1L,
                new OrderStatusUpdateRequest(
                        OrderStatus.CANCELLED
                )
        );

        verify(inventoryService, atLeastOnce())
                .restoreStockForOrder(any(), anyInt());
    }
}