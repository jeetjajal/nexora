package com.nexora.inventory.repository;

import com.nexora.inventory.entity.InventoryReservation;
import com.nexora.inventory.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {

    List<InventoryReservation> findByOrderId(Long orderId);

    List<InventoryReservation> findByOrderIdAndStatus(Long orderId, ReservationStatus status);

    @Query("SELECT r FROM InventoryReservation r WHERE r.status = :status AND r.expiresAt < :cutoff")
    List<InventoryReservation> findExpiredReservations(
            @Param("status") ReservationStatus status,
            @Param("cutoff") LocalDateTime cutoff
    );
}
