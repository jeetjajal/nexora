package com.nexora.ai.tool;

import com.nexora.auth.security.UserPrincipal;
import com.nexora.order.dto.OrderResponse;
import com.nexora.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Approved AI Tool for Order operations.
 * AI never accesses MySQL directly; it invokes these methods which enforce
 * caller authentication and resource authorization via OrderService.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAiTools {

    private final OrderService orderService;

    public String getOrderStatus(Long orderId, UserPrincipal caller) {
        if (orderId == null) {
            return "Please provide a valid numeric Order ID.";
        }
        if (caller == null) {
            return "Error: User is not authenticated. Please log in to view your order status.";
        }
        try {
            OrderResponse order = orderService.getOrderById(caller, orderId);
            return String.format("Order #%d from '%s' is currently '%s'. Placed on %s. Total amount: ₹%.2f. Delivery fee: ₹%.2f.",
                    order.getId(),
                    order.getStoreName() != null ? order.getStoreName() : "Store #" + order.getStoreId(),
                    order.getStatus(),
                    order.getCreatedAt(),
                    order.getTotalAmount(),
                    order.getDeliveryFee());
        } catch (Exception e) {
            log.warn("getOrderStatus tool error for orderId={}, user={}: {}", orderId, caller.getUsername(), e.getMessage());
            return "Unable to retrieve order #" + orderId + ". Please confirm the order number belongs to your account.";
        }
    }

    public String getOrderDetails(Long orderId, UserPrincipal caller) {
        if (orderId == null) {
            return "Please provide a valid numeric Order ID.";
        }
        if (caller == null) {
            return "Error: User is not authenticated. Please log in to view your order details.";
        }
        try {
            OrderResponse order = orderService.getOrderById(caller, orderId);
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Order #%d (%s) - Store: %s | Total: ₹%.2f\nItems:\n",
                    order.getId(),
                    order.getStatus(),
                    order.getStoreName() != null ? order.getStoreName() : "Store #" + order.getStoreId(),
                    order.getTotalAmount()));

            if (order.getItems() != null && !order.getItems().isEmpty()) {
                order.getItems().forEach(item -> sb.append(String.format("- %s x %d (₹%.2f each, Subtotal: ₹%.2f)\n",
                        item.getProductName(), item.getQuantity(), item.getUnitPrice(), item.getSubtotal())));
            } else {
                sb.append("- (No item details)\n");
            }

            if (order.getDeliveryAddressId() != null) {
                sb.append(String.format("Delivery Address ID: #%d", order.getDeliveryAddressId()));
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("getOrderDetails tool error for orderId={}, user={}: {}", orderId, caller.getUsername(), e.getMessage());
            return "Unable to access details for order #" + orderId + ": " + e.getMessage();
        }
    }

    public String getUserOrderHistory(UserPrincipal caller) {
        if (caller == null) {
            return "Error: User is not authenticated. Please log in to view your past orders.";
        }
        try {
            List<OrderResponse> orders = orderService.getMyOrders(caller, PageRequest.of(0, 5)).getContent();
            if (orders.isEmpty()) {
                return "You do not have any orders placed yet on Nexora.";
            }
            StringBuilder sb = new StringBuilder("Here are your recent Nexora orders:\n");
            for (OrderResponse o : orders) {
                sb.append(String.format("- Order #%d from '%s': ₹%.2f | Status: %s (%s)\n",
                        o.getId(),
                        o.getStoreName() != null ? o.getStoreName() : "Store #" + o.getStoreId(),
                        o.getTotalAmount(),
                        o.getStatus(),
                        o.getCreatedAt()));
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("getUserOrderHistory tool error for user={}: {}", caller.getUsername(), e.getMessage());
            return "Unable to load recent orders: " + e.getMessage();
        }
    }
}
