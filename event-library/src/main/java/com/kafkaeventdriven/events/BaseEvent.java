package com.kafkaeventdriven.events;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor
@SuperBuilder
public abstract class BaseEvent {

    private UUID eventId;
    private String eventType;
    private Instant occurredAt;
    private Instant publishedAt;
    private String source;
    private UUID correlationId;
    
    @lombok.Builder.Default
    private String version = "v1";
}