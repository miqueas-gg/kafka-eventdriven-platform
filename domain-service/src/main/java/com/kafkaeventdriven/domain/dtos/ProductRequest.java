package com.kafkaeventdriven.domain.dtos;

import java.math.BigDecimal;

public record ProductRequest(
    String name,
    BigDecimal price,
    Integer stock
) {}