package com.kafkaeventdriven.ingest.application.dtos;

public record EventStatsDTO(
    String eventType,
    Long count
) {}