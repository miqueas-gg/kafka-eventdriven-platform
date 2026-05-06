package com.kafkaeventdriven.domain.dtos;

import com.kafkaeventdriven.domain.entities.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
    UUID id,
    UUID customerId,
    OrderStatus status,
    BigDecimal totalAmount,
    String notes,
    Instant createdAt,
    List<OrderItemResponse> items
) {}
