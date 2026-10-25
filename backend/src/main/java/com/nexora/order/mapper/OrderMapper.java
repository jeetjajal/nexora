package com.nexora.order.mapper;

import com.nexora.order.dto.OrderItemResponse;
import com.nexora.order.dto.OrderResponse;
import com.nexora.order.entity.Order;
import com.nexora.order.entity.OrderItem;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class OrderMapper {

    private OrderMapper() {
    }

    public static OrderResponse toResponse(Order order, Set<OrderItem> items) {
        List<OrderItemResponse> itemResponses = items.stream()
                .sorted(Comparator.comparing(OrderItem::getId))
                .map(OrderMapper::toItemResponse)
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .storeId(order.getStore().getId())
                .storeName(order.getStore().getName())
                .deliveryAddressId(order.getDeliveryAddress().getId())
                .status(order.getStatus().name())
                .items(itemResponses)
                .subtotal(order.getSubtotal())
                .deliveryFee(order.getDeliveryFee())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private static OrderItemResponse toItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProductNameSnapshot())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .build();
    }
}
