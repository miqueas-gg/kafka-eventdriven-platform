package com.kafkaeventdriven.notification.repositories;

import com.kafkaeventdriven.notification.entities.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {
    Page<NotificationEntity> findByStatusAndEventType(String status, String eventType, Pageable pageable);
    Page<NotificationEntity> findByStatus(String status, Pageable pageable);
    Page<NotificationEntity> findByEventType(String eventType, Pageable pageable);
}