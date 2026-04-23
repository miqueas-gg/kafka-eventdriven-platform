package com.kafkaeventdriven.domain.dtos;

import com.kafkaeventdriven.domain.entities.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderItemResponse(
    UUID productId,
    int quantity,
    BigDecimal unitPrice,
    BigDecimal subtotal
) {}