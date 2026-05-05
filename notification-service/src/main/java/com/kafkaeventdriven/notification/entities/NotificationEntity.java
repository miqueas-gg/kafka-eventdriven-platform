package com.kafkaeventdriven.notification.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID originalEventId;
    private String eventType;
    private String notificationType; // Ej: EMAIL, LOG
    private String recipient;
    private String status; // SENT / FAILED
    private Integer attemptCount;
    private Instant dispatchedAt;
}