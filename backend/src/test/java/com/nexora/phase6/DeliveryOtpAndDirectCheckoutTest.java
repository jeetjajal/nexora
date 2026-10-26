package com.nexora.phase6;

import com.nexora.address.entity.Address;
import com.nexora.address.service.AddressService;
import com.nexora.auth.security.UserPrincipal;
import com.nexora.cart.repository.CartItemRepository;
import com.nexora.cart.repository.CartRepository;
import com.nexora.exception.ForbiddenOperationException;
import com.nexora.exception.ResourceNotFoundException;
import com.nexora.idempotency.service.IdempotencyService;
import com.nexora.inventory.service.InventoryService;
import com.nexora.kafka.producer.KafkaEventProducer;
import com.nexora.order.dto.*;
import com.nexora.order.entity.Order;
import com.nexora.order.entity.OrderItem;
import com.nexora.order.entity.OrderStatus;
import com.nexora.order.repository.OrderItemRepository;
import com.nexora.order.repository.OrderRepository;
import com.nexora.order.service.OrderService;
import com.nexora.product.entity.Product;
import com.nexora.product.repository.ProductRepository;
import com.nexora.role.entity.Role;
import com.nexora.role.entity.RoleName;
import com.nexora.store.entity.Store;
import com.nexora.store.repository.StoreRepository;
import com.nexora.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 6: Direct Checkout, Delivery OTP & Tracking Unit Tests")
class DeliveryOtpAndDirectCheckoutTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private AddressService addressService;
    @Mock private InventoryService inventoryService;
    @Mock private IdempotencyService idempotencyService;
    @Mock private com.nexora.delivery.service.DeliveryService deliveryService;
    @Mock private KafkaEventProducer kafkaEventProducer;
    @Mock private ProductRepository productRepository;

    private OrderService orderService;
    private User customerUser;
    private User riderUser;
    private Store store;
    private Address address;
    private Product product;

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
                idempotencyService,
                deliveryService,
                kafkaEventProducer
        );

        ReflectionTestUtils.setField(orderService, "deliveryFee", new BigDecimal("25.00"));
        ReflectionTestUtils.setField(orderService, "productRepository", productRepository);

        Role customerRole = Role.builder().id(1L).name(RoleName.CUSTOMER).build();
        Role riderRole = Role.builder().id(2L).name(RoleName.DELIVERY_PARTNER).build();

        customerUser = User.builder().id(101L).email("customer@nexora.in").name("Customer One").roles(Set.of(customerRole)).build();
        riderUser = User.builder().id(102L).email("rider@nexora.in").name("Ramesh Patel").roles(Set.of(riderRole)).build();

        store = Store.builder().id(1L).name("Nexora Dark Store SG Highway").description("SG Highway Ahmedabad").build();
        address = Address.builder().id(501L).user(customerUser).addressLine1("B-402 Shivalik").city("Ahmedabad").pincode("380054").build();
        product = Product.builder().id(1001L).name("Amul Taaza Milk 500ml").price(new BigDecimal("28.00")).store(store).available(true).build();
    }

    @Test
    @DisplayName("Direct checkout derives price authoritatively from product and reserves inventory")
    void directCheckout_Success() {
        UserPrincipal principal = new UserPrincipal(customerUser);
        DirectCheckoutRequest request = DirectCheckoutRequest.builder()
                .deliveryAddressId(501L)
                .items(List.of(DirectCheckoutItemRequest.builder().productId(1001L).quantity(2).build()))
                .paymentMethod("RAZORPAY")
                .build();

        when(addressService.findOwnedAddressOrThrow(principal, 501L)).thenReturn(address);
        when(productRepository.findById(1001L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(9901L);
            o.setCreatedAt(LocalDateTime.now());
            o.setUpdatedAt(LocalDateTime.now());
            return o;
        });

        OrderResponse response = orderService.directCheckout(principal, request, null);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(9901L);
        assertThat(response.getSubtotal()).isEqualByComparingTo("56.00"); // 28.00 * 2
        assertThat(response.getDeliveryFee()).isEqualByComparingTo("25.00");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("81.00");
        assertThat(response.getDeliveryOtp()).isNotBlank();
        assertThat(response.getDeliveryOtp()).hasSize(4);

        verify(inventoryService).reduceStockForOrder(1001L, 2);
    }

    @Test
    @DisplayName("Delivery partner verification succeeds with matching 4-digit OTP")
    void verifyDeliveryOtp_Success() {
        UserPrincipal riderPrincipal = new UserPrincipal(riderUser);
        String rawOtp = "4829";
        String otpHash = OrderService.hashOtp(rawOtp);

        Order activeOrder = Order.builder()
                .id(9901L)
                .user(customerUser)
                .store(store)
                .deliveryAddress(address)
                .status(OrderStatus.OUT_FOR_DELIVERY)
                .subtotal(new BigDecimal("56.00"))
                .deliveryFee(new BigDecimal("25.00"))
                .totalAmount(new BigDecimal("81.00"))
                .deliveryOtpHash(otpHash)
                .otpAttempts(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(9901L)).thenReturn(Optional.of(activeOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        VerifyDeliveryOtpRequest req = VerifyDeliveryOtpRequest.builder().otp("4829").build();
        OrderResponse response = orderService.verifyDeliveryOtp(riderPrincipal, 9901L, req);

        assertThat(response.getStatus()).isEqualTo("DELIVERED");
        assertThat(activeOrder.getDeliveredAt()).isNotNull();
    }

    @Test
    @DisplayName("Delivery OTP verification rejects incorrect OTP and increments attempt counter")
    void verifyDeliveryOtp_WrongOtp_IncrementsAttempts() {
        UserPrincipal riderPrincipal = new UserPrincipal(riderUser);
        String otpHash = OrderService.hashOtp("4829");

        Order activeOrder = Order.builder()
                .id(9901L)
                .user(customerUser)
                .store(store)
                .deliveryAddress(address)
                .status(OrderStatus.OUT_FOR_DELIVERY)
                .subtotal(new BigDecimal("56.00"))
                .deliveryFee(new BigDecimal("25.00"))
                .totalAmount(new BigDecimal("81.00"))
                .deliveryOtpHash(otpHash)
                .otpAttempts(0)
                .build();

        when(orderRepository.findById(9901L)).thenReturn(Optional.of(activeOrder));

        VerifyDeliveryOtpRequest req = VerifyDeliveryOtpRequest.builder().otp("1111").build();

        assertThatThrownBy(() -> orderService.verifyDeliveryOtp(riderPrincipal, 9901L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Incorrect delivery OTP");

        assertThat(activeOrder.getOtpAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("Delivery OTP locks order after 3 failed attempts")
    void verifyDeliveryOtp_ExceededAttempts_LocksOrder() {
        UserPrincipal riderPrincipal = new UserPrincipal(riderUser);
        String otpHash = OrderService.hashOtp("4829");

        Order lockedOrder = Order.builder()
                .id(9901L)
                .user(customerUser)
                .store(store)
                .deliveryAddress(address)
                .status(OrderStatus.OUT_FOR_DELIVERY)
                .subtotal(new BigDecimal("56.00"))
                .deliveryFee(new BigDecimal("25.00"))
                .totalAmount(new BigDecimal("81.00"))
                .deliveryOtpHash(otpHash)
                .otpAttempts(3)
                .build();

        when(orderRepository.findById(9901L)).thenReturn(Optional.of(lockedOrder));

        VerifyDeliveryOtpRequest req = VerifyDeliveryOtpRequest.builder().otp("4829").build();

        assertThatThrownBy(() -> orderService.verifyDeliveryOtp(riderPrincipal, 9901L, req))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("Maximum delivery OTP verification attempts exceeded");
    }

    @Test
    @DisplayName("Tracking endpoint returns authoritative timeline and status")
    void getOrderTracking_ReturnsAuthoritativeTimeline() {
        UserPrincipal customerPrincipal = new UserPrincipal(customerUser);

        Order order = Order.builder()
                .id(9901L)
                .user(customerUser)
                .store(store)
                .deliveryAddress(address)
                .status(OrderStatus.CONFIRMED)
                .subtotal(new BigDecimal("56.00"))
                .deliveryFee(new BigDecimal("25.00"))
                .totalAmount(new BigDecimal("81.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(9901L)).thenReturn(Optional.of(order));

        OrderTrackingResponse tracking = orderService.getOrderTracking(customerPrincipal, 9901L);

        assertThat(tracking).isNotNull();
        assertThat(tracking.getOrderId()).isEqualTo(9901L);
        assertThat(tracking.getStatus()).isEqualTo("CONFIRMED");
        assertThat(tracking.getTimeline()).hasSize(5);
        assertThat(tracking.getTimeline().get(0).isCompleted()).isTrue(); // PLACED
        assertThat(tracking.getTimeline().get(1).isCompleted()).isTrue(); // CONFIRMED
        assertThat(tracking.getTimeline().get(2).isCompleted()).isFalse(); // PACKING
    }
}
