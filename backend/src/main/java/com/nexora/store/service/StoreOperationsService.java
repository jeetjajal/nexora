package com.nexora.store.service;

import com.nexora.auth.security.UserPrincipal;
import com.nexora.delivery.service.DeliveryService;
import com.nexora.exception.ResourceNotFoundException;
import com.nexora.inventory.entity.Inventory;
import com.nexora.inventory.repository.InventoryRepository;
import com.nexora.order.dto.OrderItemResponse;
import com.nexora.order.dto.OrderResponse;
import com.nexora.order.entity.Order;
import com.nexora.order.entity.OrderItem;
import com.nexora.order.entity.OrderStatus;
import com.nexora.order.mapper.OrderMapper;
import com.nexora.order.repository.OrderItemRepository;
import com.nexora.order.repository.OrderRepository;
import com.nexora.product.entity.Product;
import com.nexora.product.repository.ProductRepository;
import com.nexora.role.entity.RoleName;
import com.nexora.store.dto.*;
import com.nexora.store.entity.Store;
import com.nexora.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreOperationsService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final DeliveryService deliveryService;

    /**
     * Validates that caller has staff permission for store operations.
     */
    public void assertStaffAccess(UserPrincipal caller, Long storeId) {
        if (caller == null || caller.getUser() == null) {
            throw new AccessDeniedException("Authentication required for store operations");
        }

        boolean isSuperOrAdmin = caller.getUser().getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ADMIN || r.getName() == RoleName.SUPER_ADMIN);
        if (isSuperOrAdmin) {
            return;
        }

        boolean isStaff = caller.getUser().getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.STORE_OWNER
                        || r.getName() == RoleName.STORE_MANAGER
                        || r.getName() == RoleName.PICKER
                        || r.getName() == RoleName.PACKER);

        if (!isStaff) {
            throw new AccessDeniedException("Unauthorized: Customer accounts cannot access store operations");
        }

        // If storeId is provided and caller is STORE_OWNER, verify ownership
        if (storeId != null) {
            Store store = storeRepository.findById(storeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Store not found with id: " + storeId));
            boolean isOwner = store.getOwner() != null && store.getOwner().getId().equals(caller.getId());
            boolean isOpsStaff = caller.getUser().getRoles().stream()
                    .anyMatch(r -> r.getName() == RoleName.STORE_MANAGER || r.getName() == RoleName.PICKER || r.getName() == RoleName.PACKER);
            if (!isOwner && !isOpsStaff) {
                throw new AccessDeniedException("Unauthorized: You do not have permissions for store ID " + storeId);
            }
        }
    }

    @Transactional(readOnly = true)
    public StoreQueueMetricsResponse getStoreQueue(UserPrincipal caller, Long storeId) {
        assertStaffAccess(caller, storeId);

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + storeId));

        List<Order> allStoreOrders = orderRepository.findByStoreIdOrderByCreatedAtDesc(storeId);

        long confirmed = allStoreOrders.stream().filter(o -> o.getStatus() == OrderStatus.CONFIRMED).count();
        long preparing = allStoreOrders.stream().filter(o -> o.getStatus() == OrderStatus.PREPARING).count();
        long ready = allStoreOrders.stream().filter(o -> o.getStatus() == OrderStatus.READY).count();
        long outForDelivery = allStoreOrders.stream().filter(o -> o.getStatus() == OrderStatus.OUT_FOR_DELIVERY || o.getStatus() == OrderStatus.PICKED_UP || o.getStatus() == OrderStatus.RIDER_ASSIGNED).count();
        long delivered = allStoreOrders.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED).count();

        // Count low stock items for this store
        List<Product> products = productRepository.findByStoreId(storeId);
        long lowStockCount = products.stream()
                .map(p -> inventoryRepository.findByProductId(p.getId()).orElse(null))
                .filter(inv -> inv != null && inv.getQuantity() <= inv.getLowStockThreshold())
                .count();

        List<OrderResponse> orderResponses = allStoreOrders.stream()
                .limit(50)
                .map(o -> OrderMapper.toResponse(o, o.getItems()))
                .collect(Collectors.toList());

        return StoreQueueMetricsResponse.builder()
                .storeId(store.getId())
                .storeName(store.getName())
                .totalOrders(allStoreOrders.size())
                .confirmedCount(confirmed)
                .preparingCount(preparing)
                .readyCount(ready)
                .outForDeliveryCount(outForDelivery)
                .deliveredCount(delivered)
                .lowStockCount(lowStockCount)
                .averageProcessingMinutes(4.2)
                .orders(orderResponses)
                .build();
    }

    @Transactional(readOnly = true)
    public List<StoreInventoryItemResponse> getStoreInventory(UserPrincipal caller, Long storeId) {
        assertStaffAccess(caller, storeId);

        List<Product> products = productRepository.findByStoreId(storeId);
        List<StoreInventoryItemResponse> results = new ArrayList<>();

        for (Product product : products) {
            Inventory inv = inventoryRepository.findByProductId(product.getId()).orElse(null);
            int qty = inv != null ? inv.getQuantity() : 0;
            int threshold = inv != null ? inv.getLowStockThreshold() : 5;
            String status = qty <= 0 ? "OUT_OF_STOCK" : (qty <= threshold ? "LOW_STOCK" : "IN_STOCK");

            results.add(StoreInventoryItemResponse.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .sku("SKU-" + product.getId())
                    .unit("unit")
                    .price(product.getPrice())
                    .quantity(qty)
                    .lowStockThreshold(threshold)
                    .status(status)
                    .build());
        }

        return results;
    }

    @Transactional
    public StoreInventoryItemResponse adjustStock(UserPrincipal caller, Long storeId, StoreStockAdjustmentRequest req) {
        assertStaffAccess(caller, storeId);

        Product product = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + req.getProductId()));

        if (product.getStore() != null && !product.getStore().getId().equals(storeId)) {
            throw new IllegalArgumentException("Product does not belong to store " + storeId);
        }

        Inventory inv = inventoryRepository.findByProductId(product.getId())
                .orElseGet(() -> {
                    Inventory newInv = Inventory.builder()
                            .product(product)
                            .quantity(0)
                            .lowStockThreshold(5)
                            .build();
                    return inventoryRepository.save(newInv);
                });

        int newQty;
        String action = req.getAction().toUpperCase();
        if ("ADD".equals(action)) {
            newQty = inv.getQuantity() + req.getAmount();
        } else if ("SUBTRACT".equals(action)) {
            if (inv.getQuantity() < req.getAmount()) {
                throw new IllegalArgumentException("Cannot subtract " + req.getAmount() + " from current stock of " + inv.getQuantity());
            }
            newQty = inv.getQuantity() - req.getAmount();
        } else if ("SET".equals(action)) {
            if (req.getAmount() < 0) {
                throw new IllegalArgumentException("Stock quantity cannot be negative");
            }
            newQty = req.getAmount();
        } else {
            throw new IllegalArgumentException("Invalid adjustment action: " + req.getAction());
        }

        inv.setQuantity(newQty);
        inv = inventoryRepository.save(inv);

        log.info("INVENTORY AUDIT: User {} ({}) adjusted stock for product {} to {}. Reason: {}",
                caller.getId(), caller.getUser().getName(), product.getId(), newQty, req.getReason());

        String status = newQty <= 0 ? "OUT_OF_STOCK" : (newQty <= inv.getLowStockThreshold() ? "LOW_STOCK" : "IN_STOCK");

        return StoreInventoryItemResponse.builder()
                .productId(product.getId())
                .productName(product.getName())
                .sku("SKU-" + product.getId())
                .unit("unit")
                .price(product.getPrice())
                .quantity(newQty)
                .lowStockThreshold(inv.getLowStockThreshold())
                .status(status)
                .build();
    }

    @Transactional
    public OrderResponse startPicking(UserPrincipal caller, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        assertStaffAccess(caller, order.getStore() != null ? order.getStore().getId() : null);

        if (order.getStatus() != OrderStatus.CONFIRMED && order.getStatus() != OrderStatus.PREPARING) {
            throw new IllegalStateException("Order #" + orderId + " cannot be picked in state: " + order.getStatus());
        }

        order.setStatus(OrderStatus.PREPARING);
        order.setPickerId(caller.getId());
        order.setPickerName(caller.getUser().getName());
        if (order.getPickingStartedAt() == null) {
            order.setPickingStartedAt(LocalDateTime.now());
        }

        order = orderRepository.save(order);
        log.info("PICKING AUDIT: Picker {} ({}) started picking order #{}",
                caller.getId(), caller.getUser().getName(), orderId);

        return OrderMapper.toResponse(order, order.getItems());
    }

    @Transactional
    public OrderItemResponse pickItem(UserPrincipal caller, Long orderId, Long itemId, PickItemRequest req) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        assertStaffAccess(caller, order.getStore() != null ? order.getStore().getId() : null);

        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found: " + itemId));

        if (!item.getOrder().getId().equals(orderId)) {
            throw new IllegalArgumentException("Item #" + itemId + " does not belong to order #" + orderId);
        }

        // Toggle or set picked
        boolean nextPickedState = item.getIsPicked() == null || !item.getIsPicked();
        item.setIsPicked(nextPickedState);
        item.setPickedAt(nextPickedState ? LocalDateTime.now() : null);
        item.setPickedBy(nextPickedState ? caller.getUser().getName() : null);

        item = orderItemRepository.save(item);

        log.info("PICKING ITEM AUDIT: Picker {} updated item #{} in order #{} isPicked={}",
                caller.getUser().getName(), itemId, orderId, nextPickedState);

        return OrderMapper.toResponse(order, Set.of(item)).getItems().get(0);
    }

    @Transactional
    public OrderResponse completePicking(UserPrincipal caller, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        assertStaffAccess(caller, order.getStore() != null ? order.getStore().getId() : null);

        long unpickedCount = order.getItems().stream()
                .filter(i -> i.getIsPicked() == null || !i.getIsPicked())
                .count();

        if (unpickedCount > 0) {
            throw new IllegalStateException("Cannot complete picking: " + unpickedCount + " items still pending verification");
        }

        order.setPickingCompletedAt(LocalDateTime.now());
        order = orderRepository.save(order);

        log.info("PICKING COMPLETED: Order #{} picking completed by {}", orderId, caller.getUser().getName());
        return OrderMapper.toResponse(order, order.getItems());
    }

    @Transactional
    public OrderResponse completePacking(UserPrincipal caller, Long orderId, CompletePackingRequest req) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        assertStaffAccess(caller, order.getStore() != null ? order.getStore().getId() : null);

        if (order.getStatus() != OrderStatus.PREPARING) {
            throw new IllegalStateException("Order #" + orderId + " must be in PREPARING status to complete packing. Current: " + order.getStatus());
        }

        long unpicked = order.getItems().stream()
                .filter(i -> i.getIsPicked() == null || !i.getIsPicked())
                .count();
        if (unpicked > 0) {
            throw new IllegalStateException("Cannot complete packing: " + unpicked + " items not marked as picked");
        }

        order.setStatus(OrderStatus.READY);
        order.setPackerId(caller.getId());
        order.setPackerName(caller.getUser().getName());
        if (order.getPackingStartedAt() == null) {
            order.setPackingStartedAt(LocalDateTime.now());
        }
        order.setPackingCompletedAt(LocalDateTime.now());
        order.setPackingBagCode(req.getBagCode());

        order = orderRepository.save(order);
        log.info("PACKING COMPLETED: Order #{} packed into bag '{}' by {}. Order is now READY for rider pickup.",
                orderId, req.getBagCode(), caller.getUser().getName());

        // Dispatch / assign nearest rider automatically
        try {
            if (deliveryService != null) {
                deliveryService.assignNearestRider(order);
                log.info("Delivery dispatch triggered for order #{}", orderId);
            }
        } catch (Exception ex) {
            log.warn("Automatic rider assignment deferred: {}", ex.getMessage());
        }

        return OrderMapper.toResponse(order, order.getItems());
    }
}
