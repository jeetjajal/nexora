package com.nexora.order.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nexora.address.entity.Address;
import com.nexora.address.service.AddressService;
import com.nexora.auth.security.UserPrincipal;
import com.nexora.cart.entity.Cart;
import com.nexora.cart.entity.CartItem;
import com.nexora.cart.exception.ProductUnavailableException;
import com.nexora.cart.repository.CartItemRepository;
import com.nexora.cart.repository.CartRepository;
import com.nexora.exception.ForbiddenOperationException;
import com.nexora.exception.ResourceNotFoundException;
import com.nexora.inventory.service.InventoryService;
import com.nexora.order.dto.CheckoutRequest;
import com.nexora.order.dto.OrderResponse;
import com.nexora.order.dto.OrderStatusUpdateRequest;
import com.nexora.order.entity.Order;
import com.nexora.order.entity.OrderItem;
import com.nexora.order.entity.OrderStatus;
import com.nexora.order.exception.EmptyCartException;
import com.nexora.order.exception.InvalidOrderStatusTransitionException;
import com.nexora.order.mapper.OrderMapper;
import com.nexora.order.repository.OrderItemRepository;
import com.nexora.order.repository.OrderRepository;
import com.nexora.product.entity.Product;
import com.nexora.role.entity.RoleName;
import com.nexora.store.entity.Store;
import com.nexora.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.nexora.order.entity.OrderStatus.*;

/**
 * WHERE CHECKOUT AND THE ORDER STATE MACHINE LIVE.
 *
 * checkout() is the one method that turns a Cart (Phase 7) into real
 * Orders — see its javadoc for the full multi-store-splitting and
 * stock-consumption walkthrough. Everything else here manages an
 * order's lifecycle after it exists: viewing (with per-role
 * visibility rules) and status transitions (with the state machine
 * that prevents invalid changes like DELIVERED -> PREPARING).
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final StoreRepository storeRepository;
    private final AddressService addressService;
    private final InventoryService inventoryService;
    private final com.nexora.idempotency.service.IdempotencyService idempotencyService;
    private final com.nexora.delivery.service.DeliveryService deliveryService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.nexora.redis.service.RedisCartCacheService redisCartCacheService;

    @Value("${nexora.order.delivery-fee}")
    private BigDecimal deliveryFee;

    /**
     * THE STATE MACHINE. Each key maps to the ONLY statuses an order
     * in that state may move to next; anything not listed is rejected
     * by assertValidTransition. DELIVERED, CANCELLED, and REJECTED are
     * terminal — they map to an empty set, meaning no further
     * transition is ever allowed once an order reaches one of them.
     */
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.ofEntries(
            Map.entry(CREATED, Set.of(PAYMENT_PENDING, CANCELLED)),
            Map.entry(PAYMENT_PENDING, Set.of(PAID, CANCELLED)),
            Map.entry(PAID, Set.of(CONFIRMED, REJECTED, CANCELLED)),
            Map.entry(CONFIRMED, Set.of(PREPARING, REJECTED, CANCELLED)),
            Map.entry(PREPARING, Set.of(READY, CANCELLED)),
            Map.entry(READY, Set.of(RIDER_ASSIGNED)),
            Map.entry(RIDER_ASSIGNED, Set.of(PICKED_UP)),
            Map.entry(PICKED_UP, Set.of(OUT_FOR_DELIVERY)),
            Map.entry(OUT_FOR_DELIVERY, Set.of(DELIVERED)),
            Map.entry(DELIVERED, Set.of()),
            Map.entry(CANCELLED, Set.of()),
            Map.entry(REJECTED, Set.of())
    );

    /** Statuses a CUSTOMER may still self-cancel from. */
    private static final Set<OrderStatus> CUSTOMER_CANCELLABLE_STATUSES =
            Set.of(CREATED, PAYMENT_PENDING);

    private static final String IDEMPOTENCY_SCOPE_CHECKOUT = "CHECKOUT";

    /**
     * PHASE 11:
     *
     * Idempotency-Key-aware checkout entry point.
     *
     * IMPORTANT:
     * This method itself must be transactional.
     *
     * Previously only the overloaded checkout(caller, request) method
     * had @Transactional. When this method called that overloaded
     * method internally, Spring's transaction proxy was bypassed
     * because this was a self-invocation inside the same service.
     *
     * As a result, operations such as cartItemRepository.deleteByCartId()
     * could execute without an active EntityManager transaction.
     *
     * By putting @Transactional on this entry point, the transaction
     * starts before idempotency processing and covers the complete
     * checkout operation.
     */
    @Transactional
    public List<OrderResponse> checkout(
            UserPrincipal caller,
            CheckoutRequest request,
            String idempotencyKey) {

        /*
         * Existing callers that do not provide an Idempotency-Key
         * continue to use the original checkout behavior.
         */
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return checkout(caller, request);
        }

        /*
         * The checkout request currently contains the delivery address.
         *
         * This fingerprint ensures that the same idempotency key cannot
         * later be reused with a different checkout request.
         */
        String fingerprint =
                String.valueOf(request.getDeliveryAddressId());

        return idempotencyService.executeIdempotently(
                idempotencyKey,
                IDEMPOTENCY_SCOPE_CHECKOUT,
                caller.getId(),
                fingerprint,
                new TypeReference<List<OrderResponse>>() {},
                () -> checkout(caller, request)
        );
    }

    /**
     * CHECKOUT:
     *
     * Turns the caller's Cart into one or more real Orders.
     *
     * Steps:
     *
     * 1. Resolve and ownership-check the delivery address.
     *
     * 2. Load the caller's cart.
     *
     * 3. Reject an empty cart.
     *
     * 4. Confirm that every product is still available.
     *
     * 5. Group cart items by store.
     *
     * 6. Create one Order for every distinct store.
     *
     * 7. Snapshot product price/name into OrderItem.
     *
     * 8. Reduce inventory using the concurrency-safe inventory operation.
     *
     * 9. Save OrderItems.
     *
     * 10. Update order totals.
     *
     * 11. Clear the cart only after every store succeeds.
     *
     * The complete operation is transactional.
     */
    @Transactional
    public List<OrderResponse> checkout(
            UserPrincipal caller,
            CheckoutRequest request) {

        Address deliveryAddress =
                addressService.findOwnedAddressOrThrow(
                        caller,
                        request.getDeliveryAddressId()
                );

        Cart cart = cartRepository.findByUserId(caller.getId())
                .orElseThrow(EmptyCartException::new);

        List<CartItem> cartItems =
                cartItemRepository.findByCartId(cart.getId());

        if (cartItems.isEmpty()) {
            throw new EmptyCartException();
        }

        /*
         * Make sure products have not been disabled after they were
         * added to the cart.
         */
        for (CartItem item : cartItems) {
            if (!item.getProduct().isAvailable()) {
                throw new ProductUnavailableException(
                        item.getProduct().getId()
                );
            }
        }

        /*
         * A cart may contain products from multiple stores.
         * Checkout creates one order per store.
         */
        Map<Long, List<CartItem>> itemsByStore =
                cartItems.stream()
                        .collect(Collectors.groupingBy(
                                ci -> ci.getProduct()
                                        .getStore()
                                        .getId()
                        ));

        List<OrderResponse> responses = new ArrayList<>();

        /*
         * Process each store separately.
         */
        for (List<CartItem> storeItems : itemsByStore.values()) {

            Store store =
                    storeItems.get(0)
                            .getProduct()
                            .getStore();

            Order order = Order.builder()
                    .user(caller.getUser())
                    .store(store)
                    .deliveryAddress(deliveryAddress)
                    .status(OrderStatus.CREATED)
                    .subtotal(BigDecimal.ZERO)
                    .deliveryFee(deliveryFee)
                    .totalAmount(BigDecimal.ZERO)
                    .build();

            Order savedOrder =
                    orderRepository.save(order);

            Set<OrderItem> orderItems =
                    new LinkedHashSet<>();

            BigDecimal storeSubtotal =
                    BigDecimal.ZERO;

            /*
             * Convert every CartItem into an OrderItem.
             */
            for (CartItem cartItem : storeItems) {

                Product product =
                        cartItem.getProduct();

                /*
                 * Never trust a cached cart price.
                 * Checkout uses the current product price.
                 */
                BigDecimal unitPrice =
                        product.getPrice().subtract(
                                product.getDiscount() != null
                                        ? product.getDiscount()
                                        : BigDecimal.ZERO
                        );

                BigDecimal lineSubtotal =
                        unitPrice.multiply(
                                BigDecimal.valueOf(
                                        cartItem.getQuantity()
                                )
                        );

                /*
                 * Atomically consume stock.
                 *
                 * If insufficient stock exists, InventoryService
                 * throws and the entire checkout transaction rolls back.
                 */
                inventoryService.reduceStockForOrder(
                        product.getId(),
                        cartItem.getQuantity()
                );

                OrderItem orderItem =
                        OrderItem.builder()
                                .order(savedOrder)
                                .product(product)
                                .productNameSnapshot(product.getName())
                                .unitPrice(unitPrice)
                                .quantity(cartItem.getQuantity())
                                .subtotal(lineSubtotal)
                                .build();

                orderItems.add(orderItem);
                storeSubtotal =
                        storeSubtotal.add(lineSubtotal);
            }

            /*
             * Save all order items.
             */
            orderItemRepository.saveAll(orderItems);

            /*
             * Calculate final order totals.
             */
            BigDecimal totalAmount =
                    storeSubtotal.add(deliveryFee);

            savedOrder.setItems(orderItems);
            savedOrder.setSubtotal(storeSubtotal);
            savedOrder.setTotalAmount(totalAmount);

            orderRepository.save(savedOrder);

            responses.add(
                    OrderMapper.toResponse(
                            savedOrder,
                            orderItems
                    )
            );
        }

        /*
         * Every store group succeeded.
         *
         * Because this method is transactional, deleteByCartId()
         * now executes inside an active transaction.
         */
        cartItemRepository.deleteByCartId(cart.getId());

        if (redisCartCacheService != null && caller != null && caller.getId() != null) {
            redisCartCacheService.evictCart(caller.getId());
        }

        return responses;
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(
            UserPrincipal caller,
            Long orderId) {

        Order order =
                findOrderOrThrow(orderId);

        assertCallerCanViewOrder(
                order,
                caller
        );

        return OrderMapper.toResponse(
                order,
                order.getItems()
        );
    }

    /**
     * A customer's own order history.
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(
            UserPrincipal caller,
            Pageable pageable) {

        return orderRepository
                .findByUserId(
                        caller.getId(),
                        pageable
                )
                .map(order ->
                        OrderMapper.toResponse(
                                order,
                                order.getItems()
                        )
                );
    }

    /**
     * A store owner's or ADMIN's view of orders
     * for a specific store.
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> getStoreOrders(
            UserPrincipal caller,
            Long storeId,
            Pageable pageable) {

        Store store =
                storeRepository.findById(storeId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Store not found with id: " + storeId
                                )
                        );

        assertCallerManagesStoreOrIsAdmin(
                store,
                caller
        );

        return orderRepository
                .findByStoreId(
                        storeId,
                        pageable
                )
                .map(order ->
                        OrderMapper.toResponse(
                                order,
                                order.getItems()
                        )
                );
    }

    /**
     * STORE_OWNER/ADMIN-driven order status transitions.
     */
    @Transactional
    public OrderResponse updateStatus(
            UserPrincipal caller,
            Long orderId,
            OrderStatusUpdateRequest request) {

        Order order =
                findOrderOrThrow(orderId);

        assertCallerManagesStoreOrIsAdmin(
                order.getStore(),
                caller
        );

        OrderStatus from =
                order.getStatus();

        OrderStatus to =
                request.getStatus();

        assertValidTransition(
                from,
                to
        );

        if (to == CANCELLED ||
                to == REJECTED) {

            restoreStockForOrder(order);
        }

        order.setStatus(to);

        Order saved =
                orderRepository.save(order);

        if (to == READY) {
            deliveryService.assignNearestRider(saved);
        }

        return OrderMapper.toResponse(
                saved,
                saved.getItems()
        );
    }

    /**
     * CUSTOMER self-service cancellation.
     */
    @Transactional
    public OrderResponse cancelOwnOrder(
            UserPrincipal caller,
            Long orderId) {

        Order order =
                findOrderOrThrow(orderId);

        boolean isAdmin =
                isAdmin(caller);

        boolean isOwningCustomer =
                order.getUser()
                        .getId()
                        .equals(caller.getId());

        if (!isAdmin &&
                !isOwningCustomer) {

            throw new ForbiddenOperationException(
                    "You can only cancel your own orders."
            );
        }

        OrderStatus from =
                order.getStatus();

        if (!CUSTOMER_CANCELLABLE_STATUSES
                .contains(from)) {

            throw new InvalidOrderStatusTransitionException(
                    from,
                    CANCELLED
            );
        }

        restoreStockForOrder(order);

        order.setStatus(CANCELLED);

        Order saved =
                orderRepository.save(order);

        return OrderMapper.toResponse(
                saved,
                saved.getItems()
        );
    }

    /**
     * PHASE 9:
     *
     * Called by PaymentService after a Razorpay payment order
     * has been successfully created.
     */
    @Transactional
    public void markAwaitingPayment(Order order) {

        assertValidTransition(
                order.getStatus(),
                PAYMENT_PENDING
        );

        order.setStatus(PAYMENT_PENDING);

        orderRepository.save(order);
    }

    /**
     * PHASE 9:
     *
     * Read-only lookup used by PaymentService.
     */
    @Transactional(readOnly = true)
    public Order getOrderEntityOrThrow(Long orderId) {

        return findOrderOrThrow(orderId);
    }

    /**
     * PHASE 10:
     *
     * Called after a payment has been cryptographically verified
     * as successful.
     *
     * Idempotent: if already PAID, no operation is performed.
     */
    @Transactional
    public void markPaid(Order order) {

        if (order.getStatus() == PAID) {
            return;
        }

        assertValidTransition(
                order.getStatus(),
                PAID
        );

        order.setStatus(PAID);

        orderRepository.save(order);
    }

    /**
     * PHASE 11:
     *
     * Called by PaymentService.refundPayment after the gateway
     * has actually processed the refund.
     *
     * This method deliberately performs a forgiving order-state
     * update because the payment gateway is authoritative for the
     * money movement.
     */
    @Transactional
    public void markCancelledDueToRefund(Order order) {

        OrderStatus currentStatus =
                order.getStatus();

        Set<OrderStatus> allowedNext =
                ALLOWED_TRANSITIONS.getOrDefault(
                        currentStatus,
                        Set.of()
                );

        /*
         * If the order cannot transition to CANCELLED,
         * do nothing. The refund itself has already succeeded.
         */
        if (!allowedNext.contains(CANCELLED)) {
            return;
        }

        /*
         * Return the stock that was consumed during checkout.
         */
        restoreStockForOrder(order);

        order.setStatus(CANCELLED);

        orderRepository.save(order);
    }

    // ------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------

    private Order findOrderOrThrow(Long orderId) {

        return orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found with id: " + orderId
                        )
                );
    }

    private void restoreStockForOrder(Order order) {

        for (OrderItem item : order.getItems()) {

            inventoryService.restoreStockForOrder(
                    item.getProduct().getId(),
                    item.getQuantity()
            );
        }
    }

    private void assertValidTransition(
            OrderStatus from,
            OrderStatus to) {

        Set<OrderStatus> allowedNextStatuses =
                ALLOWED_TRANSITIONS.getOrDefault(
                        from,
                        Set.of()
                );

        if (!allowedNextStatuses.contains(to)) {

            throw new InvalidOrderStatusTransitionException(
                    from,
                    to
            );
        }
    }

    private boolean isAdmin(
            UserPrincipal caller) {

        return caller.getUser()
                .getRoles()
                .stream()
                .anyMatch(role ->
                        role.getName() == RoleName.ADMIN
                );
    }

    private void assertCallerManagesStoreOrIsAdmin(
            Store store,
            UserPrincipal caller) {

        if (isAdmin(caller)) {
            return;
        }

        if (!store.getOwner()
                .getId()
                .equals(caller.getId())) {

            throw new ForbiddenOperationException(
                    "You do not have permission to manage this store's orders."
            );
        }
    }

    private void assertCallerCanViewOrder(
            Order order,
            UserPrincipal caller) {

        if (isAdmin(caller)) {
            return;
        }

        boolean isOwningCustomer =
                order.getUser()
                        .getId()
                        .equals(caller.getId());

        boolean isManagingStoreOwner =
                order.getStore()
                        .getOwner()
                        .getId()
                        .equals(caller.getId());

        if (!isOwningCustomer &&
                !isManagingStoreOwner) {

            throw new ForbiddenOperationException(
                    "You do not have permission to view this order."
            );
        }
    }
}