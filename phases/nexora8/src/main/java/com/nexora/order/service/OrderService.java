package com.nexora.order.service;

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

    @Value("${nexora.order.delivery-fee}")
    private BigDecimal deliveryFee;

    /**
     * THE STATE MACHINE. Each key maps to the ONLY statuses an order
     * in that state may move to next; anything not listed is rejected
     * by assertValidTransition. DELIVERED, CANCELLED, and REJECTED are
     * terminal — they map to an empty set, meaning no further
     * transition is ever allowed once an order reaches one of them.
     *
     * NOTE ON PAYMENT_PENDING/PAID: Phase 9/10 (Payment) doesn't exist
     * yet, so for now these transitions are reachable through the same
     * general-purpose status endpoint as everything else, available to
     * STORE_OWNER/ADMIN. Once Phase 9/10 build real payment
     * integration, PAID in particular should only ever be set by a
     * verified payment webhook, not manually — that tightening is
     * explicitly deferred, not forgotten.
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

    /** Statuses a CUSTOMER may still self-cancel from — before the store has started preparing. */
    private static final Set<OrderStatus> CUSTOMER_CANCELLABLE_STATUSES = Set.of(CREATED, PAYMENT_PENDING);

    /**
     * CHECKOUT: turns the caller's Cart into one or more real Orders.
     *
     * Steps, in order:
     *  1. Resolve and ownership-check the delivery address (reusing
     *     AddressService — a customer can only ship to their own saved
     *     address, never someone else's).
     *  2. Load the cart; reject if empty.
     *  3. Confirm every product in it is still available (a product
     *     could have been deactivated by its store after being added
     *     to the cart — Phase 7 only checked this at add-time).
     *  4. GROUP cart items by store. A cart can span multiple stores
     *     (Phase 7 placed no restriction on that); a real order is
     *     always per-store, so checkout creates one Order per distinct
     *     store represented in the cart — see Order's javadoc for why.
     *  5. For each store group, create the Order, then for each item:
     *     snapshot its current price/name into a new OrderItem, and
     *     — this is the important part — ACTUALLY consume the stock
     *     via InventoryService.reduceStockForOrder, using the exact
     *     same atomic, concurrency-safe database operation Phase 6
     *     built. This is the moment Phase 7's "friendly check, not a
     *     reservation" promise gets made real: if someone else took
     *     the last unit between add-to-cart and now, THIS is where
     *     that customer finds out, via a clean 409.
     *  6. Only after every store group succeeds, clear the cart.
     *
     * The whole method is one @Transactional unit: if ANY item in ANY
     * store group fails (insufficient stock, unavailable product), the
     * ENTIRE checkout rolls back — no partial orders, no partially
     * decremented stock, nothing left in an inconsistent state. This
     * is a deliberately simple, safe choice for this phase over a more
     * complex partial-success design.
     */
    @Transactional
    public List<OrderResponse> checkout(UserPrincipal caller, CheckoutRequest request) {
        Address deliveryAddress = addressService.findOwnedAddressOrThrow(caller, request.getDeliveryAddressId());

        Cart cart = cartRepository.findByUserId(caller.getId())
                .orElseThrow(EmptyCartException::new);

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new EmptyCartException();
        }

        for (CartItem item : cartItems) {
            if (!item.getProduct().isAvailable()) {
                throw new ProductUnavailableException(item.getProduct().getId());
            }
        }

        Map<Long, List<CartItem>> itemsByStore = cartItems.stream()
                .collect(Collectors.groupingBy(ci -> ci.getProduct().getStore().getId()));

        List<OrderResponse> responses = new ArrayList<>();

        for (List<CartItem> storeItems : itemsByStore.values()) {
            Store store = storeItems.get(0).getProduct().getStore();

            Order order = Order.builder()
                    .user(caller.getUser())
                    .store(store)
                    .deliveryAddress(deliveryAddress)
                    .status(OrderStatus.CREATED)
                    .subtotal(BigDecimal.ZERO)
                    .deliveryFee(deliveryFee)
                    .totalAmount(BigDecimal.ZERO)
                    .build();
            Order savedOrder = orderRepository.save(order);

            Set<OrderItem> orderItems = new LinkedHashSet<>();
            BigDecimal storeSubtotal = BigDecimal.ZERO;

            for (CartItem cartItem : storeItems) {
                Product product = cartItem.getProduct();

                BigDecimal unitPrice = product.getPrice().subtract(
                        product.getDiscount() != null ? product.getDiscount() : BigDecimal.ZERO);
                BigDecimal lineSubtotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

                // THE REAL, CONCURRENCY-SAFE STOCK CONSUMPTION.
                inventoryService.reduceStockForOrder(product.getId(), cartItem.getQuantity());

                OrderItem orderItem = OrderItem.builder()
                        .order(savedOrder)
                        .product(product)
                        .productNameSnapshot(product.getName())
                        .unitPrice(unitPrice)
                        .quantity(cartItem.getQuantity())
                        .subtotal(lineSubtotal)
                        .build();
                orderItemRepository.save(orderItem);
                orderItems.add(orderItem);

                storeSubtotal = storeSubtotal.add(lineSubtotal);
            }

            savedOrder.setSubtotal(storeSubtotal);
            savedOrder.setTotalAmount(storeSubtotal.add(deliveryFee));
            Order finalOrder = orderRepository.save(savedOrder);

            responses.add(OrderMapper.toResponse(finalOrder, orderItems));
        }

        // Every store group succeeded — safe to empty the cart now.
        cartItemRepository.deleteByCartId(cart.getId());

        return responses;
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UserPrincipal caller, Long orderId) {
        Order order = findOrderOrThrow(orderId);
        assertCallerCanViewOrder(order, caller);
        return OrderMapper.toResponse(order, order.getItems());
    }

    /** A customer's own order history. */
    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(UserPrincipal caller, Pageable pageable) {
        return orderRepository.findByUserId(caller.getId(), pageable)
                .map(order -> OrderMapper.toResponse(order, order.getItems()));
    }

    /** A store owner's (or admin's) view of orders placed against one specific store. */
    @Transactional(readOnly = true)
    public Page<OrderResponse> getStoreOrders(UserPrincipal caller, Long storeId, Pageable pageable) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with id: " + storeId));
        assertCallerManagesStoreOrIsAdmin(store, caller);

        return orderRepository.findByStoreId(storeId, pageable)
                .map(order -> OrderMapper.toResponse(order, order.getItems()));
    }

    /**
     * STORE_OWNER/ADMIN-driven status transitions — the broad state
     * machine (CREATED all the way through DELIVERED, plus REJECTED).
     * See ALLOWED_TRANSITIONS for exactly what's permitted from where.
     */
    @Transactional
    public OrderResponse updateStatus(UserPrincipal caller, Long orderId, OrderStatusUpdateRequest request) {
        Order order = findOrderOrThrow(orderId);
        assertCallerManagesStoreOrIsAdmin(order.getStore(), caller);

        OrderStatus from = order.getStatus();
        OrderStatus to = request.getStatus();
        assertValidTransition(from, to);

        if (to == CANCELLED || to == REJECTED) {
            restoreStockForOrder(order);
        }

        order.setStatus(to);
        Order saved = orderRepository.save(order);
        return OrderMapper.toResponse(saved, saved.getItems());
    }

    /**
     * CUSTOMER self-service cancellation — deliberately narrower than
     * updateStatus: only the customer who placed the order (or ADMIN)
     * may call this, and only while the order hasn't progressed past
     * PAYMENT_PENDING yet (once a store has confirmed/started
     * preparing, cancellation becomes a store-side decision via the
     * general status endpoint instead, reflecting that real
     * preparation may already be underway).
     */
    @Transactional
    public OrderResponse cancelOwnOrder(UserPrincipal caller, Long orderId) {
        Order order = findOrderOrThrow(orderId);

        boolean isAdmin = isAdmin(caller);
        boolean isOwningCustomer = order.getUser().getId().equals(caller.getId());
        if (!isAdmin && !isOwningCustomer) {
            throw new ForbiddenOperationException("You can only cancel your own orders.");
        }

        OrderStatus from = order.getStatus();
        if (!CUSTOMER_CANCELLABLE_STATUSES.contains(from)) {
            throw new InvalidOrderStatusTransitionException(from, CANCELLED);
        }

        restoreStockForOrder(order);
        order.setStatus(CANCELLED);
        Order saved = orderRepository.save(order);
        return OrderMapper.toResponse(saved, saved.getItems());
    }

    // ------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------

    private Order findOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
    }

    private void restoreStockForOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            inventoryService.restoreStockForOrder(item.getProduct().getId(), item.getQuantity());
        }
    }

    private void assertValidTransition(OrderStatus from, OrderStatus to) {
        Set<OrderStatus> allowedNextStatuses = ALLOWED_TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowedNextStatuses.contains(to)) {
            throw new InvalidOrderStatusTransitionException(from, to);
        }
    }

    private boolean isAdmin(UserPrincipal caller) {
        return caller.getUser().getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ADMIN);
    }

    private void assertCallerManagesStoreOrIsAdmin(Store store, UserPrincipal caller) {
        if (isAdmin(caller)) {
            return;
        }
        if (!store.getOwner().getId().equals(caller.getId())) {
            throw new ForbiddenOperationException("You do not have permission to manage this store's orders.");
        }
    }

    private void assertCallerCanViewOrder(Order order, UserPrincipal caller) {
        if (isAdmin(caller)) {
            return;
        }
        boolean isOwningCustomer = order.getUser().getId().equals(caller.getId());
        boolean isManagingStoreOwner = order.getStore().getOwner().getId().equals(caller.getId());
        if (!isOwningCustomer && !isManagingStoreOwner) {
            throw new ForbiddenOperationException("You do not have permission to view this order.");
        }
    }
}
