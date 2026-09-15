package com.nexora.delivery.service;

import com.nexora.delivery.dto.DeliveryAssignmentResponse;
import com.nexora.delivery.dto.DeliveryPartnerProfileResponse;
import com.nexora.delivery.dto.LocationUpdateRequest;
import com.nexora.delivery.entity.AssignmentStatus;
import com.nexora.delivery.entity.DeliveryAssignment;
import com.nexora.delivery.entity.DeliveryLocation;
import com.nexora.delivery.entity.DeliveryPartner;
import com.nexora.delivery.entity.DeliveryPartnerStatus;
import com.nexora.delivery.repository.DeliveryAssignmentRepository;
import com.nexora.delivery.repository.DeliveryLocationRepository;
import com.nexora.delivery.repository.DeliveryPartnerRepository;
import com.nexora.exception.ResourceNotFoundException;
import com.nexora.order.entity.Order;
import com.nexora.order.entity.OrderStatus;
import com.nexora.order.repository.OrderRepository;
import com.nexora.user.entity.User;
import com.nexora.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryPartnerRepository partnerRepository;
    private final DeliveryAssignmentRepository assignmentRepository;
    private final DeliveryLocationRepository locationRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    private static final double EARTH_RADIUS_KM = 6371.0;

    @Transactional
    public DeliveryPartnerProfileResponse getProfile(Long userId) {
        DeliveryPartner partner = getOrCreatePartner(userId);
        return mapToProfileResponse(partner);
    }

    @Transactional
    public DeliveryPartnerProfileResponse toggleOnlineStatus(Long userId, boolean isOnline) {
        DeliveryPartner partner = getOrCreatePartner(userId);
        partner.setIsOnline(isOnline);
        if (!isOnline) {
            partner.setStatus(DeliveryPartnerStatus.OFFLINE);
        } else if (partner.getStatus() == DeliveryPartnerStatus.OFFLINE) {
            partner.setStatus(DeliveryPartnerStatus.AVAILABLE);
        }
        partner = partnerRepository.save(partner);
        return mapToProfileResponse(partner);
    }

    @Transactional
    public void updateLocation(Long userId, LocationUpdateRequest request) {
        DeliveryPartner partner = getOrCreatePartner(userId);
        partner.setCurrentLat(request.getLatitude());
        partner.setCurrentLng(request.getLongitude());
        partnerRepository.save(partner);

        DeliveryLocation location = DeliveryLocation.builder()
                .deliveryPartner(partner)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();
        locationRepository.save(location);
    }

    @Transactional
    public void assignNearestRider(Order order) {
        List<DeliveryPartner> availablePartners = partnerRepository.findByStatusAndIsOnlineTrue(DeliveryPartnerStatus.AVAILABLE);

        if (availablePartners.isEmpty()) {
            log.warn("No available riders for order {}", order.getId());
            return; // In a real system, we'd queue this and retry.
        }

        // Store doesn't have an address directly in this phase, using customer delivery address as approximation for test
        double storeLat = order.getDeliveryAddress().getLatitude() != null ? order.getDeliveryAddress().getLatitude() : 0.0;
        double storeLng = order.getDeliveryAddress().getLongitude() != null ? order.getDeliveryAddress().getLongitude() : 0.0;

        DeliveryPartner nearestPartner = null;
        double minDistance = Double.MAX_VALUE;

        for (DeliveryPartner partner : availablePartners) {
            if (partner.getCurrentLat() != null && partner.getCurrentLng() != null) {
                double distance = calculateHaversineDistance(storeLat, storeLng, partner.getCurrentLat(), partner.getCurrentLng());
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestPartner = partner;
                }
            }
        }

        if (nearestPartner == null) {
            // Fallback: pick the first one if no locations are known
            nearestPartner = availablePartners.get(0);
        }

        nearestPartner.setStatus(DeliveryPartnerStatus.BUSY);
        partnerRepository.save(nearestPartner);

        DeliveryAssignment assignment = DeliveryAssignment.builder()
                .order(order)
                .deliveryPartner(nearestPartner)
                .status(AssignmentStatus.ASSIGNED)
                .build();
        assignmentRepository.save(assignment);
        
        order.setStatus(OrderStatus.RIDER_ASSIGNED);
        orderRepository.save(order);
        
        log.info("Assigned rider {} to order {} at distance {} km", nearestPartner.getId(), order.getId(), minDistance);
    }

    @Transactional
    public DeliveryAssignmentResponse acceptAssignment(Long userId, Long assignmentId) {
        DeliveryPartner partner = getOrCreatePartner(userId);
        DeliveryAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment with id " + assignmentId + " not found"));

        if (!assignment.getDeliveryPartner().getId().equals(partner.getId())) {
            throw new RuntimeException("Unauthorized to accept this assignment");
        }

        assignment.setStatus(AssignmentStatus.ACCEPTED);
        assignment.setAcceptedAt(LocalDateTime.now());
        assignment = assignmentRepository.save(assignment);
        
        return mapToAssignmentResponse(assignment);
    }

    @Transactional
    public DeliveryAssignmentResponse updateAssignmentStatus(Long userId, Long assignmentId, AssignmentStatus newStatus) {
        DeliveryPartner partner = getOrCreatePartner(userId);
        DeliveryAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment with id " + assignmentId + " not found"));

        if (!assignment.getDeliveryPartner().getId().equals(partner.getId())) {
            throw new RuntimeException("Unauthorized to update this assignment");
        }

        assignment.setStatus(newStatus);
        
        if (newStatus == AssignmentStatus.DELIVERED) {
            assignment.setCompletedAt(LocalDateTime.now());
            partner.setStatus(DeliveryPartnerStatus.AVAILABLE);
            
            // Add earnings (simple logic: fixed rate or based on order's delivery fee)
            BigDecimal deliveryFee = assignment.getOrder().getDeliveryFee() != null ? assignment.getOrder().getDeliveryFee() : BigDecimal.valueOf(5.0);
            partner.setTotalEarnings(partner.getTotalEarnings().add(deliveryFee));
            partnerRepository.save(partner);
            
            Order order = assignment.getOrder();
            order.setStatus(OrderStatus.DELIVERED);
            orderRepository.save(order);
        } else if (newStatus == AssignmentStatus.PICKED_UP) {
            Order order = assignment.getOrder();
            order.setStatus(OrderStatus.PICKED_UP);
            orderRepository.save(order);
        }

        assignment = assignmentRepository.save(assignment);
        return mapToAssignmentResponse(assignment);
    }

    private DeliveryPartner getOrCreatePartner(Long userId) {
        return partnerRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId + " not found"));
            DeliveryPartner newPartner = DeliveryPartner.builder()
                    .user(user)
                    .isOnline(false)
                    .status(DeliveryPartnerStatus.OFFLINE)
                    .totalEarnings(BigDecimal.ZERO)
                    .build();
            return partnerRepository.save(newPartner);
        });
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private DeliveryPartnerProfileResponse mapToProfileResponse(DeliveryPartner partner) {
        return DeliveryPartnerProfileResponse.builder()
                .id(partner.getId())
                .userId(partner.getUser().getId())
                .name(partner.getUser().getName())
                .isOnline(partner.getIsOnline())
                .status(partner.getStatus())
                .totalEarnings(partner.getTotalEarnings())
                .build();
    }
    
    private DeliveryAssignmentResponse mapToAssignmentResponse(DeliveryAssignment assignment) {
        return DeliveryAssignmentResponse.builder()
                .id(assignment.getId())
                .orderId(assignment.getOrder().getId())
                .partnerId(assignment.getDeliveryPartner().getId())
                .status(assignment.getStatus())
                .assignedAt(assignment.getAssignedAt())
                .acceptedAt(assignment.getAcceptedAt())
                .completedAt(assignment.getCompletedAt())
                .build();
    }
}
