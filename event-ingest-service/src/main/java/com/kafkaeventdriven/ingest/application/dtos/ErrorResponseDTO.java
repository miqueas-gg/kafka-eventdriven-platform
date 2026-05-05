package com.kafkaeventdriven.ingest.application.dtos;

import java.time.LocalDateTime;

public record ErrorResponseDTO(
    LocalDateTime timestamp,
    String message,
    String details
) {}