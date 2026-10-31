package com.nexora.delivery.service;

import com.nexora.delivery.dto.BatchOrderStopResponse;
import com.nexora.delivery.dto.DeliveryBatchResponse;
import com.nexora.delivery.entity.*;
import com.nexora.delivery.repository.DeliveryAssignmentRepository;
import com.nexora.delivery.repository.DeliveryBatchOrderRepository;
import com.nexora.delivery.repository.DeliveryBatchRepository;
import com.nexora.delivery.repository.DeliveryPartnerRepository;
import com.nexora.exception.ResourceNotFoundException;
import com.nexora.order.entity.Order;
import com.nexora.order.entity.OrderStatus;
import com.nexora.order.repository.OrderRepository;
import com.nexora.store.entity.Store;
import com.nexora.store.repository.StoreRepository;
import com.nexora.websocket.service.OrderTrackingWebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryBatchingService {

    private final DeliveryBatchRepository batchRepository;
    private final DeliveryBatchOrderRepository batchOrderRepository;
    private final DeliveryPartnerRepository partnerRepository;
    private final DeliveryAssignmentRepository assignmentRepository;
    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;

    @Autowired(required = false)
    private OrderTrackingWebSocketService orderTrackingWebSocketService;

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double DEFAULT_STORE_LAT = 23.0373;
    private static final double DEFAULT_STORE_LNG = 72.5118;
    private static final Pattern GEO_PATTERN = Pattern.compile("GEO:\\s*([0-9.-]+)\\s*,\\s*([0-9.-]+)");

    /**
     * Automatically clusters eligible pending/ready orders for a store into multi-order batches
     * based on dark-store origin, Haversine delivery destination proximity (<= maxProximityKm),
     * and rider payload capacity (<= maxBatchSize).
     */
    @Transactional
    public List<DeliveryBatchResponse> generateBatchesForStore(Long storeId, Integer maxBatchSize, Double maxProximityKm) {
        int batchLimit = (maxBatchSize != null && maxBatchSize > 0) ? maxBatchSize : 3;
        double proximityThreshold = (maxProximityKm != null && maxProximityKm > 0.0) ? maxProximityKm : 2.5;

        List<Store> targetStores = (storeId != null) 
                ? List.of(storeRepository.findById(storeId).orElseThrow(() -> new ResourceNotFoundException("Store not found: " + storeId)))
                : storeRepository.findAll();

        List<DeliveryBatchResponse> createdBatches = new ArrayList<>();

        for (Store store : targetStores) {
            List<Order> candidateOrders = orderRepository.findByStoreIdAndStatusInOrderByCreatedAtAsc(
                    store.getId(),
                    List.of(OrderStatus.READY, OrderStatus.CONFIRMED, OrderStatus.PREPARING)
            ).stream()
                    .filter(o -> !batchOrderRepository.existsByOrderId(o.getId()))
                    .filter(o -> o.getDeliveryAddress() != null && o.getDeliveryAddress().getLatitude() != null && o.getDeliveryAddress().getLongitude() != null)
                    .collect(Collectors.toList());

            if (candidateOrders.size() < 2) {
                log.info("Store #{} has only {} unbatched candidates. Skipping multi-order batch creation.", store.getId(), candidateOrders.size());
                continue;
            }

            double[] storeCoords = extractStoreCoordinates(store);
            double storeLat = storeCoords[0];
            double storeLng = storeCoords[1];

            Set<Long> processedOrderIds = new HashSet<>();

            for (Order anchorOrder : candidateOrders) {
                if (processedOrderIds.contains(anchorOrder.getId())) {
                    continue;
                }

                List<Order> cluster = new ArrayList<>();
                cluster.add(anchorOrder);
                processedOrderIds.add(anchorOrder.getId());

                double anchorLat = anchorOrder.getDeliveryAddress().getLatitude();
                double anchorLng = anchorOrder.getDeliveryAddress().getLongitude();

                for (Order candidate : candidateOrders) {
                    if (cluster.size() >= batchLimit) {
                        break;
                    }
                    if (processedOrderIds.contains(candidate.getId())) {
                        continue;
                    }

                    double candLat = candidate.getDeliveryAddress().getLatitude();
                    double candLng = candidate.getDeliveryAddress().getLongitude();

                    double distanceToAnchor = calculateHaversineDistance(anchorLat, anchorLng, candLat, candLng);
                    if (distanceToAnchor <= proximityThreshold) {
                        cluster.add(candidate);
                        processedOrderIds.add(candidate.getId());
                    }
                }

                // If we grouped 2 or more orders, create an optimized batch!
                if (cluster.size() >= 2) {
                    List<Order> optimizedStops = sequenceStopsByNearestNeighbor(storeLat, storeLng, cluster);
                    DeliveryBatch batch = persistBatch(store, optimizedStops, storeLat, storeLng);
                    createdBatches.add(mapToResponse(batch));
                    log.info("Created multi-order batch {} with {} orders for store {}", 
                            batch.getBatchCode(), cluster.size(), store.getName());
                }
            }
        }

        return createdBatches;
    }

    /**
     * Solves the Traveling Salesperson / Nearest Neighbor route for a multi-drop delivery batch:
     * Dark Store Origin -> Stop 1 (closest to store) -> Stop 2 (closest to Stop 1) -> ...
     */
    private List<Order> sequenceStopsByNearestNeighbor(double startLat, double startLng, List<Order> orders) {
        List<Order> unvisited = new ArrayList<>(orders);
        List<Order> sequence = new ArrayList<>();

        double currentLat = startLat;
        double currentLng = startLng;

        while (!unvisited.isEmpty()) {
            Order nearest = null;
            double shortestDist = Double.MAX_VALUE;

            for (Order o : unvisited) {
                double dist = calculateHaversineDistance(
                        currentLat, currentLng,
                        o.getDeliveryAddress().getLatitude(),
                        o.getDeliveryAddress().getLongitude()
                );
                if (dist < shortestDist) {
                    shortestDist = dist;
                    nearest = o;
                }
            }

            if (nearest != null) {
                sequence.add(nearest);
                unvisited.remove(nearest);
                currentLat = nearest.getDeliveryAddress().getLatitude();
                currentLng = nearest.getDeliveryAddress().getLongitude();
            } else {
                sequence.addAll(unvisited);
                break;
            }
        }

        return sequence;
    }

    private DeliveryBatch persistBatch(Store store, List<Order> sequencedOrders, double storeLat, double storeLng) {
        String batchCode = "BATCH-" + store.getId() + "-" + System.currentTimeMillis() % 1000000;

        double totalDistance = 0.0;
        double curLat = storeLat;
        double curLng = storeLng;

        for (Order o : sequencedOrders) {
            double dropLat = o.getDeliveryAddress().getLatitude();
            double dropLng = o.getDeliveryAddress().getLongitude();
            totalDistance += calculateHaversineDistance(curLat, curLng, dropLat, dropLng);
            curLat = dropLat;
            curLng = dropLng;
        }

        // ETA calculation: Urban avg 22 km/h + 3.5 minutes per delivery handover
        int estimatedMins = (int) Math.round((totalDistance / 22.0) * 60.0 + (sequencedOrders.size() * 3.5));
        if (estimatedMins < 8) estimatedMins = 8;

        DeliveryBatch batch = DeliveryBatch.builder()
                .batchCode(batchCode)
                .store(store)
                .status(DeliveryBatchStatus.CREATED)
                .orderCount(sequencedOrders.size())
                .totalDistanceKm(BigDecimal.valueOf(totalDistance).setScale(2, RoundingMode.HALF_UP))
                .estimatedDurationMins(estimatedMins)
                .build();

        batch = batchRepository.save(batch);

        LocalDateTime rollingEta = LocalDateTime.now().plusMinutes(5);
        int seq = 1;
        for (Order o : sequencedOrders) {
            rollingEta = rollingEta.plusMinutes((long) Math.ceil(estimatedMins / (double) sequencedOrders.size()));
            DeliveryBatchOrder batchOrder = DeliveryBatchOrder.builder()
                    .batch(batch)
                    .order(o)
                    .sequenceIndex(seq++)
                    .status("PENDING")
                    .estimatedDeliveryTime(rollingEta)
                    .build();
            batchOrderRepository.save(batchOrder);
            batch.getBatchOrders().add(batchOrder);
        }

        return batch;
    }

    @Transactional
    public DeliveryBatchResponse assignBatchToRider(Long batchId, Long riderPartnerId) {
        DeliveryBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery batch not found: " + batchId));

        DeliveryPartner partner = partnerRepository.findById(riderPartnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Rider not found: " + riderPartnerId));

        batch.setDeliveryPartner(partner);
        batch.setStatus(DeliveryBatchStatus.ASSIGNED);
        batchRepository.save(batch);

        List<DeliveryBatchOrder> batchOrders = batchOrderRepository.findByBatchIdOrderBySequenceIndexAsc(batchId);
        for (DeliveryBatchOrder bo : batchOrders) {
            Order order = bo.getOrder();
            order.setStatus(OrderStatus.RIDER_ASSIGNED);
            orderRepository.save(order);

            // Create or update DeliveryAssignment
            DeliveryAssignment assignment = DeliveryAssignment.builder()
                    .order(order)
                    .deliveryPartner(partner)
                    .status(AssignmentStatus.ASSIGNED)
                    .build();
            assignmentRepository.save(assignment);

            // WebSocket event
            if (orderTrackingWebSocketService != null) {
                String riderName = (partner.getUser() != null) ? partner.getUser().getName() : "Rider #" + partner.getId();
                orderTrackingWebSocketService.broadcastOrderStatusUpdate(
                        order, OrderStatus.RIDER_ASSIGNED,
                        "Batch Assigned: " + riderName + " will deliver stop #" + bo.getSequenceIndex()
                );
            }
        }

        return mapToResponse(batch);
    }

    @Transactional
    public DeliveryBatchResponse dispatchBatch(Long batchId) {
        DeliveryBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery batch not found: " + batchId));

        batch.setStatus(DeliveryBatchStatus.IN_TRANSIT);
        batch.setDispatchedAt(LocalDateTime.now());
        batchRepository.save(batch);

        List<DeliveryBatchOrder> batchOrders = batchOrderRepository.findByBatchIdOrderBySequenceIndexAsc(batchId);
        for (DeliveryBatchOrder bo : batchOrders) {
            bo.setStatus("IN_TRANSIT");
            batchOrderRepository.save(bo);

            Order order = bo.getOrder();
            order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
            orderRepository.save(order);

            if (orderTrackingWebSocketService != null) {
                orderTrackingWebSocketService.broadcastOrderStatusUpdate(
                        order, OrderStatus.OUT_FOR_DELIVERY,
                        "Your order is out for delivery in optimized batch " + batch.getBatchCode() + " (Stop #" + bo.getSequenceIndex() + ")"
                );
            }
        }

        return mapToResponse(batch);
    }

    @Transactional
    public DeliveryBatchResponse cancelBatch(Long batchId) {
        DeliveryBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery batch not found: " + batchId));

        batch.setStatus(DeliveryBatchStatus.CANCELLED);
        batchRepository.save(batch);

        List<DeliveryBatchOrder> batchOrders = batchOrderRepository.findByBatchIdOrderBySequenceIndexAsc(batchId);
        for (DeliveryBatchOrder bo : batchOrders) {
            bo.setStatus("CANCELLED");
            batchOrderRepository.save(bo);

            Order order = bo.getOrder();
            if (order.getStatus() == OrderStatus.RIDER_ASSIGNED || order.getStatus() == OrderStatus.READY) {
                order.setStatus(OrderStatus.READY);
                orderRepository.save(order);
            }
        }

        return mapToResponse(batch);
    }

    @Transactional(readOnly = true)
    public List<DeliveryBatchResponse> getAllBatches() {
        return batchRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DeliveryBatchResponse> getBatchesByStore(Long storeId) {
        return batchRepository.findByStoreId(storeId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DeliveryBatchResponse getBatchById(Long batchId) {
        DeliveryBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery batch not found: " + batchId));
        return mapToResponse(batch);
    }

    private DeliveryBatchResponse mapToResponse(DeliveryBatch batch) {
        List<DeliveryBatchOrder> batchOrders = batchOrderRepository.findByBatchIdOrderBySequenceIndexAsc(batch.getId());

        List<BatchOrderStopResponse> stops = batchOrders.stream().map(bo -> {
            Order o = bo.getOrder();
            String addressStr = (o.getDeliveryAddress() != null)
                    ? (o.getDeliveryAddress().getAddressLine1() + ", " + o.getDeliveryAddress().getCity())
                    : "Address N/A";
            Double lat = (o.getDeliveryAddress() != null) ? o.getDeliveryAddress().getLatitude() : null;
            Double lng = (o.getDeliveryAddress() != null) ? o.getDeliveryAddress().getLongitude() : null;
            String custName = (o.getUser() != null) ? o.getUser().getName() : "Customer #" + o.getId();
            String custPhone = (o.getUser() != null) ? o.getUser().getPhone() : "";

            return BatchOrderStopResponse.builder()
                    .orderId(o.getId())
                    .orderNumber("NX-" + o.getId())
                    .sequenceIndex(bo.getSequenceIndex())
                    .customerName(custName)
                    .customerPhone(custPhone)
                    .deliveryAddress(addressStr)
                    .latitude(lat)
                    .longitude(lng)
                    .totalAmount(o.getTotalAmount())
                    .status(bo.getStatus())
                    .estimatedDeliveryTime(bo.getEstimatedDeliveryTime())
                    .actualDeliveryTime(bo.getActualDeliveryTime())
                    .build();
        }).collect(Collectors.toList());

        String riderName = null;
        String riderPhone = null;
        Long riderId = null;
        if (batch.getDeliveryPartner() != null) {
            riderId = batch.getDeliveryPartner().getId();
            if (batch.getDeliveryPartner().getUser() != null) {
                riderName = batch.getDeliveryPartner().getUser().getName();
                riderPhone = batch.getDeliveryPartner().getUser().getPhone();
            } else {
                riderName = "Rider #" + riderId;
            }
        }

        return DeliveryBatchResponse.builder()
                .id(batch.getId())
                .batchCode(batch.getBatchCode())
                .storeId(batch.getStore().getId())
                .storeName(batch.getStore().getName())
                .deliveryPartnerId(riderId)
                .riderName(riderName)
                .riderPhone(riderPhone)
                .status(batch.getStatus())
                .orderCount(batch.getOrderCount())
                .totalDistanceKm(batch.getTotalDistanceKm())
                .estimatedDurationMins(batch.getEstimatedDurationMins())
                .stops(stops)
                .dispatchedAt(batch.getDispatchedAt())
                .completedAt(batch.getCompletedAt())
                .createdAt(batch.getCreatedAt())
                .updatedAt(batch.getUpdatedAt())
                .build();
    }

    private double[] extractStoreCoordinates(Store store) {
        if (store.getDescription() != null) {
            Matcher matcher = GEO_PATTERN.matcher(store.getDescription());
            if (matcher.find()) {
                try {
                    double lat = Double.parseDouble(matcher.group(1));
                    double lng = Double.parseDouble(matcher.group(2));
                    return new double[]{lat, lng};
                } catch (Exception ignored) {
                }
            }
        }
        return new double[]{DEFAULT_STORE_LAT, DEFAULT_STORE_LNG};
    }

    public double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
