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
        return toResponse(order, items, null);
    }

    public static OrderResponse toResponse(Order order, Set<OrderItem> items, String deliveryOtp) {
        List<OrderItemResponse> itemResponses = items.stream()
                .sorted(Comparator.comparing(OrderItem::getId))
                .map(OrderMapper::toItemResponse)
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .storeId(order.getStore() != null ? order.getStore().getId() : null)
                .storeName(order.getStore() != null ? order.getStore().getName() : "Nexora Dark Store")
                .deliveryAddressId(order.getDeliveryAddress() != null ? order.getDeliveryAddress().getId() : null)
                .status(order.getStatus() != null ? order.getStatus().name() : "CREATED")
                .items(itemResponses)
                .subtotal(order.getSubtotal())
                .deliveryFee(order.getDeliveryFee())
                .totalAmount(order.getTotalAmount())
                .deliveryOtp(deliveryOtp)
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .pickerId(order.getPickerId())
                .pickerName(order.getPickerName())
                .pickingStartedAt(order.getPickingStartedAt())
                .pickingCompletedAt(order.getPickingCompletedAt())
                .packerId(order.getPackerId())
                .packerName(order.getPackerName())
                .packingStartedAt(order.getPackingStartedAt())
                .packingCompletedAt(order.getPackingCompletedAt())
                .packingBagCode(order.getPackingBagCode())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private static OrderItemResponse toItemResponse(OrderItem item) {
        String sku = item.getProduct() != null ? "SKU-" + item.getProduct().getId() : "SKU-" + item.getId();
        String unit = "unit";
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .productName(item.getProductNameSnapshot())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .isPicked(item.getIsPicked() != null ? item.getIsPicked() : false)
                .pickedAt(item.getPickedAt())
                .pickedBy(item.getPickedBy())
                .sku(sku)
                .unit(unit)
                .build();
    }
}
