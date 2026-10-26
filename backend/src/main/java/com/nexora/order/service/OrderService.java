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
import com.nexora.kafka.producer.KafkaEventProducer;
import com.nexora.redis.service.RedisCartCacheService;
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

import com.nexora.order.dto.DirectCheckoutRequest;
import com.nexora.order.dto.DirectCheckoutItemRequest;
import com.nexora.order.dto.VerifyDeliveryOtpRequest;
import com.nexora.order.dto.OrderTrackingResponse;
import com.nexora.order.dto.TrackingTimelineEvent;
import com.nexora.product.repository.ProductRepository;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
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
    private final KafkaEventProducer kafkaEventProducer;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private RedisCartCacheService redisCartCacheService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.nexora.websocket.service.OrderTrackingWebSocketService orderTrackingWebSocketService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ProductRepository productRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.nexora.notification.service.NotificationService notificationService;

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

            // Phase 16: Publish OrderCreatedEvent to Kafka
            try {
                kafkaEventProducer.publishOrderCreated(savedOrder);
            } catch (Exception ex) {
                // Non-blocking asynchronous dispatch
            }

            // Phase 17: Real-time WebSocket Order Tracking
            try {
                if (orderTrackingWebSocketService != null) {
                    orderTrackingWebSocketService.broadcastOrderStatusUpdate(savedOrder, null, "Order placed successfully!");
                }
            } catch (Exception ex) {
                // Non-blocking
            }

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

        // Phase 16: Publish Kafka events for transitions
        try {
            if (to == CONFIRMED) {
                kafkaEventProducer.publishOrderConfirmed(saved);
            } else if (to == READY) {
                kafkaEventProducer.publishOrderReady(saved);
            } else if (to == CANCELLED || to == REJECTED) {
                kafkaEventProducer.publishOrderCancelled(saved, "Status transitioned to " + to, caller.getUsername());
            }
        } catch (Exception ex) {
            // Non-blocking asynchronous dispatch
        }

        // Phase 17: Real-time WebSocket Order Tracking
        try {
            if (orderTrackingWebSocketService != null) {
                orderTrackingWebSocketService.broadcastOrderStatusUpdate(saved, from, null);
            }
        } catch (Exception ex) {
            // Non-blocking
        }

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

        // Phase 16: Publish OrderCancelledEvent
        try {
            kafkaEventProducer.publishOrderCancelled(saved, "Cancelled by customer", caller.getUsername());
        } catch (Exception ex) {
            // Non-blocking asynchronous dispatch
        }

        // Phase 17: Real-time WebSocket Order Tracking
        try {
            if (orderTrackingWebSocketService != null) {
                orderTrackingWebSocketService.broadcastOrderStatusUpdate(saved, from, "Cancelled by customer");
            }
        } catch (Exception ex) {
            // Non-blocking
        }

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

        OrderStatus from = order.getStatus();
        order.setStatus(PAID);

        Order saved = orderRepository.save(order);

        // Phase 17: Real-time WebSocket Order Tracking
        try {
            if (orderTrackingWebSocketService != null) {
                orderTrackingWebSocketService.broadcastOrderStatusUpdate(saved, from, "Payment verified! Order is confirmed.");
            }
        } catch (Exception ex) {
            // Non-blocking
        }
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

        Order saved = orderRepository.save(order);

        // Phase 17: Real-time WebSocket Order Tracking
        try {
            if (orderTrackingWebSocketService != null) {
                orderTrackingWebSocketService.broadcastOrderStatusUpdate(saved, currentStatus, "Order cancelled due to refund.");
            }
        } catch (Exception ex) {
            // Non-blocking
        }
    }

    // ------------------------------------------------------------

    // ============================================================
    // PHASE 6: DIRECT CHECKOUT, DELIVERY OTP & REAL-TIME TRACKING
    // ============================================================

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static String generate4DigitOtp() {
        int code = 1000 + SECURE_RANDOM.nextInt(9000);
        return String.valueOf(code);
    }

    public static String hashOtp(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otp.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    @Transactional
    public OrderResponse directCheckout(
            UserPrincipal caller,
            DirectCheckoutRequest request,
            String idempotencyKey) {

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String fingerprint = request.getDeliveryAddressId() + "_" + (request.getItems() != null ? request.getItems().size() : 0);
            return idempotencyService.executeIdempotently(
                    idempotencyKey,
                    IDEMPOTENCY_SCOPE_CHECKOUT,
                    caller.getId(),
                    fingerprint,
                    new TypeReference<OrderResponse>() {},
                    () -> directCheckoutInternal(caller, request)
            );
        }

        return directCheckoutInternal(caller, request);
    }

    private OrderResponse directCheckoutInternal(
            UserPrincipal caller,
            DirectCheckoutRequest request) {

        Address deliveryAddress = addressService.findOwnedAddressOrThrow(
                caller,
                request.getDeliveryAddressId()
        );

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new EmptyCartException();
        }

        if (productRepository == null) {
            throw new IllegalStateException("ProductRepository is not available");
        }

        Product firstProduct = productRepository.findById(request.getItems().get(0).getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getItems().get(0).getProductId()));

        Store store = firstProduct.getStore();
        if (store == null) {
            store = storeRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("No active store found"));
        }

        String rawOtp = generate4DigitOtp();
        String otpHash = hashOtp(rawOtp);

        Order order = Order.builder()
                .user(caller.getUser())
                .store(store)
                .deliveryAddress(deliveryAddress)
                .status(OrderStatus.CREATED)
                .subtotal(BigDecimal.ZERO)
                .deliveryFee(deliveryFee != null ? deliveryFee : new BigDecimal("25.00"))
                .totalAmount(BigDecimal.ZERO)
                .deliveryOtpHash(otpHash)
                .otpAttempts(0)
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "RAZORPAY")
                .paymentStatus("PENDING")
                .build();

        Order savedOrder = orderRepository.save(order);

        Set<OrderItem> orderItems = new LinkedHashSet<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (DirectCheckoutItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemReq.getProductId()));

            if (!product.isAvailable()) {
                throw new ProductUnavailableException(product.getId());
            }

            inventoryService.reduceStockForOrder(product.getId(), itemReq.getQuantity());

            BigDecimal unitPrice = product.getPrice().subtract(
                    product.getDiscount() != null ? product.getDiscount() : BigDecimal.ZERO
            );

            BigDecimal lineSubtotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .product(product)
                    .productNameSnapshot(product.getName())
                    .unitPrice(unitPrice)
                    .quantity(itemReq.getQuantity())
                    .subtotal(lineSubtotal)
                    .build();

            orderItems.add(orderItem);
            subtotal = subtotal.add(lineSubtotal);
        }

        orderItemRepository.saveAll(orderItems);

        BigDecimal calculatedFee = subtotal.compareTo(new BigDecimal("199.00")) >= 0 ? BigDecimal.ZERO : (deliveryFee != null ? deliveryFee : new BigDecimal("25.00"));
        BigDecimal discount = BigDecimal.ZERO;
        if ("NEXORA50".equalsIgnoreCase(request.getCouponCode()) && subtotal.compareTo(new BigDecimal("200.00")) >= 0) {
            discount = new BigDecimal("50.00");
        }

        BigDecimal finalTotal = subtotal.add(calculatedFee).subtract(discount).max(BigDecimal.ZERO);

        savedOrder.setItems(orderItems);
        savedOrder.setSubtotal(subtotal);
        savedOrder.setDeliveryFee(calculatedFee);
        savedOrder.setTotalAmount(finalTotal);

        Order finalizedOrder = orderRepository.save(savedOrder);

        try {
            kafkaEventProducer.publishOrderCreated(finalizedOrder);
        } catch (Exception ex) {}

        try {
            if (orderTrackingWebSocketService != null) {
                orderTrackingWebSocketService.broadcastOrderStatusUpdate(finalizedOrder, null, "Order placed successfully!");
            }
        } catch (Exception ex) {}

        try {
            if (notificationService != null) {
                notificationService.notifyOrderConfirmed(finalizedOrder);
            }
        } catch (Exception ex) {}

        return OrderMapper.toResponse(finalizedOrder, orderItems, rawOtp);
    }

    @Transactional
    public OrderResponse verifyDeliveryOtp(
            UserPrincipal caller,
            Long orderId,
            VerifyDeliveryOtpRequest request) {

        Order order = findOrderOrThrow(orderId);

        boolean isDeliveryPartner = caller.getUser().getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.DELIVERY_PARTNER || r.getName() == RoleName.ADMIN || r.getName() == RoleName.STORE_OWNER);

        if (!isDeliveryPartner) {
            throw new ForbiddenOperationException("Only delivery partners or administrators can verify delivery OTP.");
        }

        if (order.getStatus() == OrderStatus.DELIVERED) {
            return OrderMapper.toResponse(order, order.getItems());
        }

        int attempts = order.getOtpAttempts() != null ? order.getOtpAttempts() : 0;
        if (attempts >= 3) {
            throw new ForbiddenOperationException("Maximum delivery OTP verification attempts exceeded (3/3). Please contact Dark Store Manager.");
        }

        String inputHash = hashOtp(request.getOtp().trim());
        if (!inputHash.equals(order.getDeliveryOtpHash())) {
            order.setOtpAttempts(attempts + 1);
            orderRepository.save(order);
            int remaining = 2 - attempts;
            throw new IllegalArgumentException("Incorrect delivery OTP. " + remaining + " attempts remaining.");
        }

        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(java.time.LocalDateTime.now());
        order.setOtpAttempts(0);
        Order saved = orderRepository.save(order);

        try {
            if (orderTrackingWebSocketService != null) {
                orderTrackingWebSocketService.broadcastOrderStatusUpdate(saved, OrderStatus.OUT_FOR_DELIVERY, "Order successfully delivered! Thank you for choosing Nexora.");
            }
        } catch (Exception ex) {}

        try {
            if (notificationService != null) {
                notificationService.notifyOrderDelivered(saved);
            }
        } catch (Exception ex) {}

        return OrderMapper.toResponse(saved, saved.getItems());
    }

    @Transactional(readOnly = true)
    public OrderTrackingResponse getOrderTracking(
            UserPrincipal caller,
            Long orderId) {

        Order order = findOrderOrThrow(orderId);
        assertCallerCanViewOrder(order, caller);

        List<TrackingTimelineEvent> timeline = buildTrackingTimeline(order);

        return OrderTrackingResponse.builder()
                .orderId(order.getId())
                .status(order.getStatus().name())
                .storeName(order.getStore() != null ? order.getStore().getName() : "Nexora Dark Store SG Highway")
                .storeAddress(order.getStore() != null ? order.getStore().getDescription() : "Ground Floor, Shapath V, SG Highway, Ahmedabad")
                .deliveryAddress(order.getDeliveryAddress() != null ? 
                        order.getDeliveryAddress().getAddressLine1() + ", " + order.getDeliveryAddress().getCity() : "Customer Address")
                .estimatedDeliveryMinutes(estimateDeliveryMinutes(order.getStatus()))
                .assignedRiderName("Ramesh Patel")
                .assignedRiderPhone("+91 98765 43210")
                .assignedRiderVehicle("Hero Electric NYX EV")
                .deliveryOtp(null)
                .timeline(timeline)
                .totalAmount(order.getTotalAmount())
                .paymentStatus(order.getPaymentStatus())
                .paymentMethod(order.getPaymentMethod())
                .createdAt(order.getCreatedAt())
                .deliveredAt(order.getDeliveredAt())
                .build();
    }

    private int estimateDeliveryMinutes(OrderStatus status) {
        return switch (status) {
            case CREATED, PAYMENT_PENDING, PAID -> 10;
            case CONFIRMED, PREPARING -> 8;
            case READY, RIDER_ASSIGNED -> 6;
            case PICKED_UP, OUT_FOR_DELIVERY -> 4;
            case DELIVERED, CANCELLED, REJECTED -> 0;
        };
    }

    private List<TrackingTimelineEvent> buildTrackingTimeline(Order order) {
        OrderStatus current = order.getStatus();
        java.time.LocalDateTime baseTime = order.getCreatedAt() != null ? order.getCreatedAt() : java.time.LocalDateTime.now();

        int currentStage = getStageIndex(current);

        return List.of(
                TrackingTimelineEvent.builder()
                        .status("PLACED")
                        .title("Order Placed")
                        .description("Order received at " + (order.getStore() != null ? order.getStore().getName() : "Nexora Dark Store"))
                        .timestamp(baseTime)
                        .completed(currentStage >= 1)
                        .current(currentStage == 1)
                        .build(),
                TrackingTimelineEvent.builder()
                        .status("CONFIRMED")
                        .title("Order Confirmed")
                        .description("Stock reserved and allocated to store picking queue")
                        .timestamp(baseTime.plusMinutes(1))
                        .completed(currentStage >= 2)
                        .current(currentStage == 2)
                        .build(),
                TrackingTimelineEvent.builder()
                        .status("PACKING")
                        .title("Packing Essentials")
                        .description("Dark store picker scanning items and sealing bag")
                        .timestamp(baseTime.plusMinutes(3))
                        .completed(currentStage >= 3)
                        .current(currentStage == 3)
                        .build(),
                TrackingTimelineEvent.builder()
                        .status("OUT_FOR_DELIVERY")
                        .title("Out for Delivery")
                        .description("Rider Ramesh Patel en route with EV scooter")
                        .timestamp(baseTime.plusMinutes(5))
                        .completed(currentStage >= 4)
                        .current(currentStage == 4)
                        .build(),
                TrackingTimelineEvent.builder()
                        .status("DELIVERED")
                        .title("Delivered")
                        .description("Handed over at doorstep via secure OTP verification")
                        .timestamp(order.getDeliveredAt() != null ? order.getDeliveredAt() : baseTime.plusMinutes(9))
                        .completed(currentStage >= 5)
                        .current(currentStage == 5)
                        .build()
        );
    }

    private int getStageIndex(OrderStatus status) {
        return switch (status) {
            case CREATED, PAYMENT_PENDING -> 1;
            case PAID, CONFIRMED -> 2;
            case PREPARING, READY -> 3;
            case RIDER_ASSIGNED, PICKED_UP, OUT_FOR_DELIVERY -> 4;
            case DELIVERED -> 5;
            case CANCELLED, REJECTED -> 0;
        };
    }

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
                        role.getName() == RoleName.ADMIN || role.getName() == RoleName.SUPER_ADMIN
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
                order.getStore() != null &&
                order.getStore().getOwner() != null &&
                order.getStore().getOwner().getId() != null &&
                order.getStore().getOwner().getId().equals(caller.getId());

        if (!isOwningCustomer &&
                !isManagingStoreOwner) {

            throw new ForbiddenOperationException(
                    "You do not have permission to view this order."
            );
        }
    }
}