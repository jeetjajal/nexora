package com.nexora.phase9;

import com.nexora.address.entity.Address;
import com.nexora.auth.security.UserPrincipal;
import com.nexora.exception.ForbiddenOperationException;
import com.nexora.order.entity.Order;
import com.nexora.order.entity.OrderStatus;
import com.nexora.order.service.OrderService;
import com.nexora.payment.dto.PaymentResponse;
import com.nexora.payment.dto.PaymentResultRequest;
import com.nexora.payment.entity.Payment;
import com.nexora.payment.entity.PaymentStatus;
import com.nexora.payment.exception.InvalidPaymentStateException;
import com.nexora.payment.gateway.PaymentGatewayClient;
import com.nexora.payment.gateway.RazorpayOrderResult;
import com.nexora.payment.repository.PaymentRepository;
import com.nexora.payment.repository.PaymentTransactionRepository;
import com.nexora.payment.service.PaymentService;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService unit tests (Phase 9)")
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private OrderService orderService;
    @Mock private PaymentGatewayClient gatewayClient;

    private PaymentService paymentService;

    private User customer;
    private User otherCustomer;
    private User adminUser;
    private Order order;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, paymentTransactionRepository, orderService, gatewayClient);
        ReflectionTestUtils.setField(paymentService, "razorpayKeyId", "rzp_test_fake_key_id");

        Role customerRole = Role.builder().id(1L).name(RoleName.CUSTOMER).build();
        Role adminRole = Role.builder().id(2L).name(RoleName.ADMIN).build();

        customer = User.builder().id(10L).name("Customer").email("customer@example.com")
                .password("hashed").status("ACTIVE").roles(Set.of(customerRole)).build();
        otherCustomer = User.builder().id(11L).name("Other Customer").email("other@example.com")
                .password("hashed").status("ACTIVE").roles(Set.of(customerRole)).build();
        adminUser = User.builder().id(99L).name("Admin").email("admin@example.com")
                .password("hashed").status("ACTIVE").roles(Set.of(adminRole)).build();

        User owner = User.builder().id(20L).name("Owner").email("owner@example.com")
                .password("hashed").status("ACTIVE")
                .roles(Set.of(Role.builder().id(3L).name(RoleName.STORE_OWNER).build())).build();
        Store store = Store.builder().id(1L).owner(owner).name("Test Store").build();
        Address address = Address.builder().id(1L).user(customer).city("Rajkot").build();

        order = Order.builder()
                .id(100L).user(customer).store(store).deliveryAddress(address)
                .status(OrderStatus.CREATED)
                .subtotal(new BigDecimal("200.00"))
                .deliveryFee(new BigDecimal("40.00"))
                .totalAmount(new BigDecimal("240.00"))
                .build();
    }

    // ==============================================================
    // Initiate payment
    // ==============================================================

    @Test
    @DisplayName("Initiating payment for a CREATED order creates a Payment and moves order to PAYMENT_PENDING")
    void initiatePaymentSucceeds() {
        when(orderService.getOrderEntityOrThrow(100L)).thenReturn(order);
        when(paymentRepository.findByOrderId(100L)).thenReturn(List.of());
        when(gatewayClient.createOrder(any(BigDecimal.class), anyString(), anyString()))
                .thenReturn(new RazorpayOrderResult("order_test_abc123", "created"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        PaymentResponse response = paymentService.initiatePayment(new UserPrincipal(customer), 100L);

        assertThat(response.getRazorpayOrderId()).isEqualTo("order_test_abc123");
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getAmount()).isEqualByComparingTo("240.00");
        assertThat(response.getRazorpayKeyId()).isEqualTo("rzp_test_fake_key_id");

        verify(orderService).markAwaitingPayment(order);
        verify(paymentTransactionRepository).save(any());
    }

    @Test
    @DisplayName("Initiating payment for an already-PAID order throws InvalidPaymentStateException")
    void initiatePaymentForAlreadyPaidOrderThrows() {
        Payment successfulPayment = Payment.builder().id(5L).order(order).status(PaymentStatus.SUCCESS)
                .razorpayOrderId("order_old").amount(new BigDecimal("240.00")).build();

        when(orderService.getOrderEntityOrThrow(100L)).thenReturn(order);
        when(paymentRepository.findByOrderId(100L)).thenReturn(List.of(successfulPayment));

        assertThatThrownBy(() -> paymentService.initiatePayment(new UserPrincipal(customer), 100L))
                .isInstanceOf(InvalidPaymentStateException.class)
                .hasMessageContaining("already been paid");

        verify(gatewayClient, never()).createOrder(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Initiating payment for an order not in CREATED status throws InvalidPaymentStateException")
    void initiatePaymentForNonCreatedOrderThrows() {
        order.setStatus(OrderStatus.CANCELLED);

        when(orderService.getOrderEntityOrThrow(100L)).thenReturn(order);
        when(paymentRepository.findByOrderId(100L)).thenReturn(List.of());

        assertThatThrownBy(() -> paymentService.initiatePayment(new UserPrincipal(customer), 100L))
                .isInstanceOf(InvalidPaymentStateException.class)
                .hasMessageContaining("CANCELLED");

        verify(gatewayClient, never()).createOrder(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Initiating payment when a PENDING payment already exists returns that SAME payment, not a new one")
    void initiatePaymentReturnsExistingPendingPayment() {
        Payment existingPending = Payment.builder().id(7L).order(order).status(PaymentStatus.PENDING)
                .razorpayOrderId("order_already_pending").amount(new BigDecimal("240.00")).build();

        when(orderService.getOrderEntityOrThrow(100L)).thenReturn(order);
        when(paymentRepository.findByOrderId(100L)).thenReturn(List.of(existingPending));

        PaymentResponse response = paymentService.initiatePayment(new UserPrincipal(customer), 100L);

        assertThat(response.getRazorpayOrderId()).isEqualTo("order_already_pending");
        verify(gatewayClient, never()).createOrder(any(), anyString(), anyString());
        verify(paymentRepository, never()).save(any());
        verify(orderService, never()).markAwaitingPayment(any());
    }

    @Test
    @DisplayName("A different customer cannot initiate payment for someone else's order")
    void otherCustomerCannotInitiatePayment() {
        when(orderService.getOrderEntityOrThrow(100L)).thenReturn(order);

        assertThatThrownBy(() -> paymentService.initiatePayment(new UserPrincipal(otherCustomer), 100L))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(gatewayClient, never()).createOrder(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("ADMIN can initiate payment for any order")
    void adminCanInitiatePaymentForAnyOrder() {
        when(orderService.getOrderEntityOrThrow(100L)).thenReturn(order);
        when(paymentRepository.findByOrderId(100L)).thenReturn(List.of());
        when(gatewayClient.createOrder(any(BigDecimal.class), anyString(), anyString()))
                .thenReturn(new RazorpayOrderResult("order_admin_created", "created"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(2L);
            return p;
        });

        PaymentResponse response = paymentService.initiatePayment(new UserPrincipal(adminUser), 100L);

        assertThat(response.getRazorpayOrderId()).isEqualTo("order_admin_created");
    }

    // ==============================================================
    // Client-reported result — the "never trust the frontend" guarantee
    // ==============================================================

    @Test
    @DisplayName("Recording a client-reported result logs it but NEVER changes the payment status to SUCCESS")
    void clientReportedResultNeverMarksSuccess() {
        Payment payment = Payment.builder().id(1L).order(order).status(PaymentStatus.PENDING)
                .razorpayOrderId("order_abc").amount(new BigDecimal("240.00")).build();

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        PaymentResultRequest request = new PaymentResultRequest("pay_fake123", "fake_signature_value");
        PaymentResponse response = paymentService.recordClientReportedResult(
                new UserPrincipal(customer), 1L, request);

        // Still PENDING — nothing about this call is allowed to mark it SUCCESS
        assertThat(response.getStatus()).isEqualTo("PENDING");
        verify(paymentTransactionRepository).save(argThat(tx ->
                tx.getRazorpayPaymentId().equals("pay_fake123")));

        // The Payment entity itself was never saved/mutated by this operation
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("A different customer cannot report a result for someone else's payment")
    void otherCustomerCannotReportResult() {
        Payment payment = Payment.builder().id(1L).order(order).status(PaymentStatus.PENDING)
                .razorpayOrderId("order_abc").amount(new BigDecimal("240.00")).build();

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.recordClientReportedResult(
                new UserPrincipal(otherCustomer), 1L, new PaymentResultRequest("pay_x", "sig_x")))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}
