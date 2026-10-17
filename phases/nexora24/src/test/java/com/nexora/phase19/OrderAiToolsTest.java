package com.nexora.phase19;

import com.nexora.ai.tool.OrderAiTools;
import com.nexora.auth.security.UserPrincipal;
import com.nexora.order.dto.OrderItemResponse;
import com.nexora.order.dto.OrderResponse;
import com.nexora.order.service.OrderService;
import com.nexora.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderAiToolsTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderAiTools orderAiTools;

    private UserPrincipal testUser;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .id(1L)
                .name("Alice")
                .email("alice@nexora.test")
                .roles(Set.of())
                .build();
        testUser = new UserPrincipal(user);
    }

    @Test
    @DisplayName("getOrderStatus: returns formatted order status when caller is authorized")
    void getOrderStatus_Success() {
        OrderResponse response = OrderResponse.builder()
                .id(101L)
                .storeId(5L)
                .storeName("Domino's")
                .status("OUT_FOR_DELIVERY")
                .createdAt(LocalDateTime.now())
                .totalAmount(BigDecimal.valueOf(350.0))
                .deliveryFee(BigDecimal.valueOf(40.0))
                .build();

        when(orderService.getOrderById(eq(testUser), eq(101L))).thenReturn(response);

        String result = orderAiTools.getOrderStatus(101L, testUser);

        assertThat(result).contains("Order #101");
        assertThat(result).contains("OUT_FOR_DELIVERY");
        assertThat(result).contains("Domino's");
    }

    @Test
    @DisplayName("getOrderStatus: returns error message when caller is unauthenticated")
    void getOrderStatus_Unauthenticated() {
        String result = orderAiTools.getOrderStatus(101L, null);
        assertThat(result).contains("Error: User is not authenticated");
    }

    @Test
    @DisplayName("getOrderDetails: formats order item breakdown and subtotal")
    void getOrderDetails_Success() {
        OrderItemResponse item = OrderItemResponse.builder()
                .productName("Farmhouse Pizza")
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(250.0))
                .subtotal(BigDecimal.valueOf(500.0))
                .build();

        OrderResponse response = OrderResponse.builder()
                .id(101L)
                .storeName("Pizza Hut")
                .status("PREPARING")
                .totalAmount(BigDecimal.valueOf(540.0))
                .items(List.of(item))
                .deliveryAddressId(20L)
                .build();

        when(orderService.getOrderById(eq(testUser), eq(101L))).thenReturn(response);

        String result = orderAiTools.getOrderDetails(101L, testUser);

        assertThat(result).contains("Order #101");
        assertThat(result).contains("Farmhouse Pizza x 2");
        assertThat(result).contains("₹500.00");
    }

    @Test
    @DisplayName("getUserOrderHistory: lists customer's recent orders")
    void getUserOrderHistory_Success() {
        OrderResponse o1 = OrderResponse.builder()
                .id(101L)
                .storeName("Domino's")
                .totalAmount(BigDecimal.valueOf(450.0))
                .status("DELIVERED")
                .createdAt(LocalDateTime.now())
                .build();

        when(orderService.getMyOrders(eq(testUser), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(o1)));

        String result = orderAiTools.getUserOrderHistory(testUser);

        assertThat(result).contains("recent Nexora orders");
        assertThat(result).contains("Order #101");
        assertThat(result).contains("DELIVERED");
    }
}
