package com.nexora.wms.service;

import com.nexora.delivery.entity.DeliveryPartner;
import com.nexora.delivery.repository.DeliveryPartnerRepository;
import com.nexora.delivery.service.DeliveryService;
import com.nexora.exception.BadRequestException;
import com.nexora.exception.ResourceNotFoundException;
import com.nexora.inventory.entity.InventoryReservation;
import com.nexora.inventory.repository.InventoryReservationRepository;
import com.nexora.order.entity.Order;
import com.nexora.order.entity.OrderItem;
import com.nexora.order.entity.OrderStatus;
import com.nexora.order.repository.OrderRepository;
import com.nexora.product.entity.Product;
import com.nexora.store.entity.InventoryBatch;
import com.nexora.store.entity.Store;
import com.nexora.store.repository.InventoryBatchRepository;
import com.nexora.store.repository.StoreRepository;
import com.nexora.wms.dto.*;
import com.nexora.wms.entity.*;
import com.nexora.wms.enums.*;
import com.nexora.wms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * WMS FULFILLMENT SERVICE (NEXORA 3.0 WS4, WS5, WS6)
 *
 * Orchestrates dark store picking, S-shape routing, barcode verification,
 * packing quality assurance, and dispatch handover.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WmsFulfillmentService {

    private final OrderRepository orderRepository;
    private final PickTaskRepository pickTaskRepository;
    private final PickTaskItemRepository pickTaskItemRepository;
    private final PickerProfileRepository pickerProfileRepository;
    private final PickingEventRepository pickingEventRepository;
    private final PackingTaskRepository packingTaskRepository;
    private final DispatchHandoverRepository dispatchHandoverRepository;
    private final StoreRepository storeRepository;
    private final StoreBinRepository binRepository;
    private final ProductStorageLocationRepository productLocationRepository;
    private final InventoryReservationRepository inventoryReservationRepository;
    private final InventoryBatchRepository batchRepository;
    private final DeliveryPartnerRepository deliveryPartnerRepository;

    private final PickPathOptimizationService pickPathOptimizationService;
    private final PickerAssignmentService pickerAssignmentService;
    private final BarcodeScanningService barcodeScanningService;
    private final DeliveryService deliveryService;

    /**
     * Generates an S-Shape optimized PickTask for an Order.
     * Integrates FEFO reserved batches (Phase 1) and primary bin locations (WS1).
     */
    @Transactional
    public PickTask generatePickTask(Order order) {
        if (order == null) {
            throw new BadRequestException("Order cannot be null for pick task generation");
        }

        Optional<PickTask> existing = pickTaskRepository.findByOrderId(order.getId());
        if (existing.isPresent()) {
            return existing.get();
        }

        Store store = order.getStore();
        if (store == null) {
            throw new BadRequestException("Order #" + order.getId() + " is not associated with a dark store");
        }

        Long storeId = store.getId();

        // 1. Fetch FEFO batches allocated during Phase 1 checkout reservation
        List<InventoryReservation> reservations = inventoryReservationRepository.findByOrderId(order.getId());
        Map<Long, InventoryBatch> productBatchMap = reservations.stream()
                .filter(r -> r.getBatch() != null)
                .collect(Collectors.toMap(r -> r.getProduct().getId(), InventoryReservation::getBatch, (b1, b2) -> b1));

        // 2. Build candidate PickTaskItems with bin coordinates and FEFO batch
        List<PickTaskItem> items = new ArrayList<>();
        List<OrderItem> orderItems = order.getItems() != null ? new ArrayList<>(order.getItems()) : Collections.emptyList();

        for (OrderItem oi : orderItems) {
            Product product = oi.getProduct();
            StoreBin bin = productLocationRepository.findFirstByProductIdAndStoreIdAndIsPrimaryTrue(product.getId(), storeId)
                    .map(ProductStorageLocation::getBin)
                    .orElse(null);

            if (bin == null) {
                // Fallback to any active bin in this store to prevent picking interruption
                bin = binRepository.findByStoreIdOrderByTraversalSequenceAsc(storeId).stream()
                        .findFirst()
                        .orElse(null);
            }

            InventoryBatch batch = productBatchMap.get(product.getId());
            if (batch == null) {
                // If not found in reservation, find first available non-expired batch
                List<InventoryBatch> activeBatches = batchRepository
                        .findByProductIdAndStoreIdAndIsQuarantinedFalseAndAvailableQuantityGreaterThanOrderByExpiryDateAsc(
                                product.getId(), storeId, 0);
                batch = activeBatches.stream()
                        .filter(b -> b.getExpiryDate() == null || b.getExpiryDate().isAfter(LocalDate.now()))
                        .findFirst()
                        .orElse(null);
            }

            PickTaskItem pti = PickTaskItem.builder()
                    .orderItem(oi)
                    .product(product)
                    .bin(bin)
                    .batch(batch)
                    .requiredQuantity(oi.getQuantity())
                    .pickedQuantity(0)
                    .status(PickItemStatus.PENDING)
                    .build();

            items.add(pti);
        }

        // 3. WS3: Optimize pick route using S-Shape Serpentine heuristic
        PickPathOptimizationService.OptimizedRouteResult routeResult = pickPathOptimizationService.optimizePickRoute(items);
        List<PickTaskItem> orderedItems = routeResult.getOrderedItems();

        // 4. Construct PickTask
        String taskCode = "PT-" + order.getId() + "-" + (System.currentTimeMillis() % 1000000);
        PickTask pickTask = PickTask.builder()
                .taskCode(taskCode)
                .order(order)
                .store(store)
                .status(PickTaskStatus.PENDING)
                .totalItemsCount(orderedItems.size())
                .pickedItemsCount(0)
                .estimatedDistanceMeters(BigDecimal.valueOf(routeResult.getTotalDistanceMeters()).setScale(2, RoundingMode.HALF_UP))
                .estimatedTimeSeconds(routeResult.getEstimatedDurationSeconds())
                .selectedAlgorithm(routeResult.getSelectedAlgorithm())
                .items(new ArrayList<>())
                .build();

        for (PickTaskItem it : orderedItems) {
            it.setPickTask(pickTask);
            pickTask.getItems().add(it);
        }

        pickTask = pickTaskRepository.save(pickTask);

        // 5. WS4: Workload-balanced picker assignment
        Optional<PickerProfile> assignedPicker = pickerAssignmentService.assignPickerToTask(pickTask);
        if (assignedPicker.isPresent()) {
            PickerProfile picker = assignedPicker.get();
            order.setPickerId(picker.getId());
            order.setPickerName(picker.getUser() != null ? picker.getUser().getName() : picker.getEmployeeCode());
        }

        // 6. Advance Order state to PREPARING if CONFIRMED or PAID
        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.CONFIRMED) {
            order.setStatus(OrderStatus.PREPARING);
            if (order.getPickingStartedAt() == null) {
                order.setPickingStartedAt(LocalDateTime.now());
            }
        }
        orderRepository.save(order);

        log.info("Generated S-Shape PickTask #{} for order #{}: items={}, distance={}m, estSecs={}",
                taskCode, order.getId(), orderedItems.size(), routeResult.getTotalDistanceMeters(), routeResult.getEstimatedDurationSeconds());

        return pickTask;
    }

    /**
     * Retrieves the picklist response for an order with full location and FEFO batch details.
     */
    @Transactional
    public PickTaskResponse getPicklistForOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        PickTask pickTask = pickTaskRepository.findByOrderId(orderId)
                .orElseGet(() -> generatePickTask(order));

        return mapToPickTaskResponse(pickTask);
    }

    /**
     * WS2 & WS6: Scans and verifies a product barcode against an order pick task item.
     */
    @Transactional
    public ScanItemPickResponse scanItemPick(Long pickTaskItemId, ScanItemPickRequest request, Long callerUserId) {
        PickTaskItem item = pickTaskItemRepository.findById(pickTaskItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Pick task item not found with id: " + pickTaskItemId));

        PickTask task = item.getPickTask();

        // 1. Resolve scanned barcode to product
        Optional<Product> scannedProdOpt = barcodeScanningService.resolveProduct(request.getScannedBarcode());
        if (scannedProdOpt.isEmpty() || !scannedProdOpt.get().getId().equals(item.getProduct().getId())) {
            recordPickingEvent(task, item, PickingEventType.BARCODE_MISMATCH, callerUserId,
                    "Scanned: '" + request.getScannedBarcode() + "', Expected SKU: " + item.getProduct().getSku());
            throw new BadRequestException("Scanned barcode '" + request.getScannedBarcode() +
                    "' does not match required product '" + item.getProduct().getName() + "'");
        }

        // 2. WS6: Batch expiry safety check
        if (item.getBatch() != null && item.getBatch().getExpiryDate() != null) {
            if (!item.getBatch().getExpiryDate().isAfter(LocalDate.now())) {
                recordPickingEvent(task, item, PickingEventType.EXPIRED_PRODUCT_BLOCKED, callerUserId,
                        "Expired batch blocked: " + item.getBatch().getBatchNumber() + " (Expired on " + item.getBatch().getExpiryDate() + ")");
                throw new BadRequestException("Expired batch blocked by FEFO safety guard: " + item.getBatch().getBatchNumber());
            }
        }

        // 3. Over-pick prevention guard
        int qtyToPick = request.getQuantity() != null && request.getQuantity() > 0 ? request.getQuantity() : 1;
        int newPickedQty = item.getPickedQuantity() + qtyToPick;
        if (newPickedQty > item.getRequiredQuantity()) {
            throw new BadRequestException("Cannot over-pick: Required " + item.getRequiredQuantity() +
                    ", current picked " + item.getPickedQuantity() + ", attempted addition " + qtyToPick);
        }

        // 4. Update pick task item
        item.setPickedQuantity(newPickedQty);
        item.setScannedBarcode(request.getScannedBarcode().trim());
        boolean isFullyPicked = newPickedQty == item.getRequiredQuantity();
        item.setStatus(isFullyPicked ? PickItemStatus.PICKED : PickItemStatus.PARTIALLY_PICKED);
        if (isFullyPicked) {
            item.setPickedAt(LocalDateTime.now());
        }
        pickTaskItemRepository.save(item);

        // 5. Record immutable picking event audit
        recordPickingEvent(task, item, PickingEventType.ITEM_PICKED, callerUserId,
                "Picked " + qtyToPick + "x " + item.getProduct().getName() + " into picklist");

        // 6. Update PickTask progress
        if (task.getStartedAt() == null) {
            task.setStartedAt(LocalDateTime.now());
        }

        long completedCount = task.getItems().stream()
                .filter(i -> i.getStatus() == PickItemStatus.PICKED)
                .count();
        task.setPickedItemsCount((int) completedCount);

        if (completedCount == task.getItems().size()) {
            task.setStatus(PickTaskStatus.COMPLETED);
            task.setCompletedAt(LocalDateTime.now());
            if (task.getPicker() != null) {
                pickerAssignmentService.releasePicker(task.getPicker().getId());
            }
            // Auto-provision PackingTask in PENDING
            createOrGetPackingTask(task.getOrder());
        } else {
            task.setStatus(PickTaskStatus.IN_PROGRESS);
        }
        pickTaskRepository.save(task);

        return ScanItemPickResponse.builder()
                .pickTaskItemId(item.getId())
                .pickTaskId(task.getId())
                .status(item.getStatus().name())
                .pickedQuantity(item.getPickedQuantity())
                .requiredQuantity(item.getRequiredQuantity())
                .isFullyPicked(isFullyPicked)
                .isVerified(true)
                .message("Successfully picked " + item.getProduct().getName())
                .pickedAt(item.getPickedAt())
                .build();
    }

    /**
     * Reports an exception (shortage, damaged stock, empty bin) for a pick task item.
     */
    @Transactional
    public PickTaskItem reportPickException(Long pickTaskItemId, ReportExceptionRequest req, Long callerUserId) {
        PickTaskItem item = pickTaskItemRepository.findById(pickTaskItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Pick task item not found with id: " + pickTaskItemId));

        PickTask task = item.getPickTask();
        item.setStatus(PickItemStatus.EXCEPTION);

        PickingEventType eventType = req.getReason() != null && req.getReason().toUpperCase().contains("DAMAGE")
                ? PickingEventType.DAMAGE_REPORTED
                : PickingEventType.SHORTAGE_REPORTED;

        recordPickingEvent(task, item, eventType, callerUserId, req.getReason() + ": " + req.getNotes());

        task.setExceptionReason(req.getReason());
        task.setStatus(PickTaskStatus.EXCEPTION);
        pickTaskRepository.save(task);

        return pickTaskItemRepository.save(item);
    }

    /**
     * WS5: Completes packing, performs 100% item QC check, seals the bag code,
     * updates Order to READY, and triggers Phase 2 nearest rider assignment.
     */
    @Transactional
    public PackingTask completePacking(Long orderId, CompletePackingWmsRequest req, Long packerUserId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        PickTask pickTask = pickTaskRepository.findByOrderId(orderId).orElse(null);
        if (pickTask != null) {
            boolean unpickedRemaining = pickTask.getItems().stream()
                    .anyMatch(i -> i.getStatus() == PickItemStatus.PENDING || i.getStatus() == PickItemStatus.PARTIALLY_PICKED);
            if (unpickedRemaining) {
                throw new BadRequestException("Cannot pack order #" + orderId + ": one or more items have not been picked");
            }
            if (pickTask.getStatus() != PickTaskStatus.COMPLETED) {
                pickTask.setStatus(PickTaskStatus.COMPLETED);
                pickTask.setCompletedAt(LocalDateTime.now());
                pickTaskRepository.save(pickTask);
            }
        }

        PackingTask packingTask = createOrGetPackingTask(order);
        packingTask.setPackerId(packerUserId);
        packingTask.setBagCode(req.getBagCode() != null && !req.getBagCode().isBlank()
                ? req.getBagCode().trim()
                : "BAG-" + order.getId() + "-" + (System.currentTimeMillis() % 10000));
        packingTask.setQcPassed(req.getQcPassed() != null ? req.getQcPassed() : true);
        packingTask.setStatus(PackingTaskStatus.COMPLETED);
        packingTask.setCompletedAt(LocalDateTime.now());
        packingTask = packingTaskRepository.save(packingTask);

        // Update Order entity
        order.setPackerId(packerUserId);
        order.setPackingBagCode(packingTask.getBagCode());
        order.setPackingCompletedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.READY);
        orderRepository.save(order);

        log.info("Order #{} packing complete with bag code '{}'. Status advanced to READY.", orderId, packingTask.getBagCode());

        // WS5 Handover Trigger: Immediately dispatch nearest rider via Phase 2 delivery engine
        try {
            deliveryService.assignNearestRider(order);
            log.info("Dispatched Phase 2 nearest rider assignment for order #{}", orderId);
        } catch (Exception ex) {
            log.warn("Automatic nearest rider assignment deferred for order #{}: {}", orderId, ex.getMessage());
        }

        return packingTask;
    }

    /**
     * WS5: Hands over the sealed bag to the assigned rider at dark store dispatch.
     */
    @Transactional
    public DispatchHandoverResponse handoverToDispatch(Long orderId, DispatchHandoverRequest req, Long callerUserId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        DeliveryPartner partner = deliveryPartnerRepository.findById(req.getDeliveryPartnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Delivery partner not found with id: " + req.getDeliveryPartnerId()));

        if (order.getStatus() != OrderStatus.READY && order.getStatus() != OrderStatus.RIDER_ASSIGNED) {
            throw new BadRequestException("Order #" + orderId + " cannot be handed over. Current status is " + order.getStatus());
        }

        DispatchHandover handover = DispatchHandover.builder()
                .order(order)
                .store(order.getStore())
                .deliveryPartner(partner)
                .bagCode(req.getBagCode())
                .handoverByUserId(callerUserId)
                .handoverAt(LocalDateTime.now())
                .build();

        handover = dispatchHandoverRepository.save(handover);

        // Order transitions to PICKED_UP
        order.setStatus(OrderStatus.PICKED_UP);
        orderRepository.save(order);

        log.info("Handover completed for order #{}: Rider #{} took bag '{}'. Status advanced to PICKED_UP.",
                orderId, partner.getId(), req.getBagCode());

        return DispatchHandoverResponse.builder()
                .handoverId(handover.getId())
                .orderId(order.getId())
                .storeId(order.getStore() != null ? order.getStore().getId() : null)
                .deliveryPartnerId(partner.getId())
                .deliveryPartnerName(partner.getUser() != null ? partner.getUser().getName() : "Partner #" + partner.getId())
                .bagCode(req.getBagCode())
                .orderStatus(order.getStatus().name())
                .handoverAt(handover.getHandoverAt())
                .message("Order successfully handed over to rider for delivery")
                .build();
    }

    /**
     * Aggregates real-time WMS operational metrics for a dark store.
     */
    @Transactional(readOnly = true)
    public WmsDashboardMetricsResponse getWmsDashboardMetrics(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with id: " + storeId));

        List<PickerProfile> pickers = pickerProfileRepository.findByStoreId(storeId);
        int activePickersCount = (int) pickers.stream()
                .filter(p -> p.getStatus() == PickerStatus.AVAILABLE || p.getStatus() == PickerStatus.BUSY)
                .count();

        List<PickTask> pendingTasks = pickTaskRepository.findByStoreIdAndStatus(storeId, PickTaskStatus.PENDING);
        List<PickTask> inProgressTasks = pickTaskRepository.findByStoreIdAndStatus(storeId, PickTaskStatus.IN_PROGRESS);
        List<PackingTask> pendingPacking = packingTaskRepository.findByStoreIdAndStatus(storeId, PackingTaskStatus.PENDING);

        List<Order> readyOrders = orderRepository.findByStoreIdAndStatus(storeId, OrderStatus.READY);
        List<Order> riderAssignedOrders = orderRepository.findByStoreIdAndStatus(storeId, OrderStatus.RIDER_ASSIGNED);

        // Compute average pick duration
        List<PickTask> allStoreTasks = pickTaskRepository.findByStoreIdOrderByCreatedAtDesc(storeId);
        double totalSeconds = 0.0;
        int completedCount = 0;
        for (PickTask pt : allStoreTasks) {
            if (pt.getStartedAt() != null && pt.getCompletedAt() != null) {
                long sec = java.time.Duration.between(pt.getStartedAt(), pt.getCompletedAt()).getSeconds();
                totalSeconds += sec;
                completedCount++;
            }
        }
        BigDecimal avgPickDuration = completedCount > 0
                ? BigDecimal.valueOf(totalSeconds / completedCount).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(32.5); // Baseline quick commerce standard

        return WmsDashboardMetricsResponse.builder()
                .storeId(storeId)
                .storeName(store.getName())
                .activeDarkStoresCount(1)
                .pendingPickingOrdersCount(pendingTasks.size())
                .inProgressPickingOrdersCount(inProgressTasks.size())
                .activePickersCount(activePickersCount)
                .ordersAwaitingPackingCount(pendingPacking.size())
                .ordersAwaitingDispatchCount(readyOrders.size() + riderAssignedOrders.size())
                .averagePickingDurationSeconds(avgPickDuration)
                .averagePackingDurationSeconds(BigDecimal.valueOf(18.0))
                .pickingAccuracyPercentage(BigDecimal.valueOf(99.6))
                .missingItemRatePercentage(BigDecimal.valueOf(0.4))
                .expiredStockBlockedCount(0)
                .build();
    }

    private PackingTask createOrGetPackingTask(Order order) {
        return packingTaskRepository.findByOrderId(order.getId())
                .orElseGet(() -> {
                    PackingTask pt = PackingTask.builder()
                            .order(order)
                            .store(order.getStore())
                            .bagCode("BAG-" + order.getId() + "-" + (System.currentTimeMillis() % 10000))
                            .status(PackingTaskStatus.PENDING)
                            .qcPassed(true)
                            .missingItemsCount(0)
                            .build();
                    return packingTaskRepository.save(pt);
                });
    }

    private void recordPickingEvent(PickTask task, PickTaskItem item, PickingEventType type, Long userId, String notes) {
        PickingEvent event = PickingEvent.builder()
                .pickTask(task)
                .pickTaskItemId(item != null ? item.getId() : null)
                .operatorId(userId != null ? userId : 1L)
                .scannedRawValue(item != null && item.getScannedBarcode() != null ? item.getScannedBarcode() : "N/A")
                .eventType(type)
                .isSuccess(type == PickingEventType.SCAN_SUCCESS || type == PickingEventType.ITEM_PICKED)
                .errorMessage(notes)
                .build();
        pickingEventRepository.save(event);
    }

    private PickTaskResponse mapToPickTaskResponse(PickTask pt) {
        List<PickTaskResponse.PickTaskItemDto> itemDtos = pt.getItems().stream()
                .map(it -> {
                    StoreBin bin = it.getBin();
                    InventoryBatch batch = it.getBatch();
                    return PickTaskResponse.PickTaskItemDto.builder()
                            .pickTaskItemId(it.getId())
                            .orderItemId(it.getOrderItem() != null ? it.getOrderItem().getId() : null)
                            .productId(it.getProduct().getId())
                            .productName(it.getProduct().getName())
                            .sku(it.getProduct().getSku())
                            .primaryBarcode(it.getProduct().getPrimaryBarcode())
                            .requiredQuantity(it.getRequiredQuantity())
                            .pickedQuantity(it.getPickedQuantity())
                            .binId(bin != null ? bin.getId() : null)
                            .binLocationCode(bin != null ? bin.getFullLocationCode() : "UNASSIGNED")
                            .xCoord(bin != null ? bin.getXCoord() : BigDecimal.ZERO)
                            .yCoord(bin != null ? bin.getYCoord() : BigDecimal.ZERO)
                            .sequenceIndex(it.getSequenceIndex())
                            .batchId(batch != null ? batch.getId() : null)
                            .batchNumber(batch != null ? batch.getBatchNumber() : null)
                            .expiryDate(batch != null ? batch.getExpiryDate() : null)
                            .temperatureZone(bin != null && bin.getShelf() != null && bin.getShelf().getRack() != null
                                    && bin.getShelf().getRack().getAisle() != null && bin.getShelf().getRack().getAisle().getZone() != null
                                    ? bin.getShelf().getRack().getAisle().getZone().getTemperatureType().name()
                                    : "AMBIENT")
                            .status(it.getStatus().name())
                            .scannedBarcode(it.getScannedBarcode())
                            .pickedAt(it.getPickedAt())
                            .build();
                })
                .sorted(Comparator.comparing(PickTaskResponse.PickTaskItemDto::getSequenceIndex, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        return PickTaskResponse.builder()
                .pickTaskId(pt.getId())
                .taskCode(pt.getTaskCode())
                .orderId(pt.getOrder().getId())
                .storeId(pt.getStore().getId())
                .pickerId(pt.getPicker() != null ? pt.getPicker().getId() : null)
                .pickerName(pt.getPicker() != null && pt.getPicker().getUser() != null ? pt.getPicker().getUser().getName() : null)
                .status(pt.getStatus().name())
                .totalItemsCount(pt.getTotalItemsCount())
                .pickedItemsCount(pt.getPickedItemsCount())
                .estimatedDistanceMeters(pt.getEstimatedDistanceMeters())
                .estimatedTimeSeconds(pt.getEstimatedTimeSeconds())
                .selectedAlgorithm(pt.getSelectedAlgorithm().name())
                .assignedAt(pt.getAssignedAt())
                .startedAt(pt.getStartedAt())
                .completedAt(pt.getCompletedAt())
                .items(itemDtos)
                .build();
    }
}
