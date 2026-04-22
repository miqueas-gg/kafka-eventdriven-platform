package com.kafkaeventdriven.domain.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderRequest(
    @NotNull(message = "El cliente es obligatorio")
    UUID customerId,

    @NotEmpty(message = "El pedido debe tener al menos un item")
    @Valid // Importante para validar los objetos dentro de la lista
    List<OrderItemRequest> items,

    String notes
) {}

