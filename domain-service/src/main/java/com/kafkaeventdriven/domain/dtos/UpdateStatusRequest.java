package com.kafkaeventdriven.domain.dtos;

public record UpdateStatusRequest(
    String newStatus,
    String reason
) {}