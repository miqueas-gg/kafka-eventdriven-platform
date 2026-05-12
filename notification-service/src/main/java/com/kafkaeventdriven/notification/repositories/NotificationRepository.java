package com.kafkaeventdriven.notification.repositories;

import com.kafkaeventdriven.notification.entities.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.attemptCount = n.attemptCount + 1 WHERE n.originalEventId = :eventId")
    void incrementAttemptCount(@Param("eventId") UUID eventId);
    Optional<NotificationEntity> findByOriginalEventId(UUID originalEventId);
    List<NotificationEntity> findAllByOriginalEventId(UUID originalEventId);
    Page<NotificationEntity> findByStatusAndEventType(String status, String eventType, Pageable pageable);
    Page<NotificationEntity> findByStatus(String status, Pageable pageable);
    Page<NotificationEntity> findByEventType(String eventType, Pageable pageable);
}