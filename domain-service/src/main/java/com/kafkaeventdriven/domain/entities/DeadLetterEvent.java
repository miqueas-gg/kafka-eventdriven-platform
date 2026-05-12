package com.kafkaeventdriven.domain.entities;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dead_letter_events", schema = "domain")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeadLetterEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String originalTopic;
    private UUID eventId;
    private String eventType;
    
    @Column(columnDefinition = "TEXT")
    private String payload;
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    private Instant failedAt;
    private Integer retryCount;
}
