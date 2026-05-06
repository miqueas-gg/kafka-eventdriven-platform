package com.kafkaeventdriven.ingest.application.dtos;

import java.time.Instant;

import java.util.UUID;

public record EventResponseDTO(
    UUID id,                // ID interno (Primary Key)
    UUID eventId,           // ID de negocio (el de Kafka)
    String eventType,
    String source,
    Object payload,         // El JSONB completo
    Instant occurredAt,
    Instant ingestedAt
) {}