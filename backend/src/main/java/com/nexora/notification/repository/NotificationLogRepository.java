package com.nexora.notification.repository;

import com.nexora.notification.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    boolean existsByDeduplicationKey(String deduplicationKey);
    Optional<NotificationLog> findByDeduplicationKey(String deduplicationKey);
    List<NotificationLog> findByOrderIdOrderByCreatedAtDesc(Long orderId);
    List<NotificationLog> findByRecipientOrderByCreatedAtDesc(String recipient);
    List<NotificationLog> findAllByOrderByCreatedAtDesc();
}
